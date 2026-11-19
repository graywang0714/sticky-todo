import { createClient, type SupabaseClient, type Session } from '@supabase/supabase-js'
import { Ledger, ROOT, spaceId } from './ledger'
import type {Kind,Op,Row} from './ledger'
import {AccountApi,reconcile} from './desktop-sync'

export interface Todo {
  id: string
  text: string
  done: boolean
  pinned: boolean
  position: number
  created_at: number
  completed_at: number | null
}

export interface Note {
  id: string
  title: string
  content: string
  pinned: boolean
  updated_at: number
  created_at: number
}

export type SyncStatus = 'local' | 'connecting' | 'online' | 'error' | 'need-auth'

const LS_CFG = 'sticky-todo-supabase'

export interface SupabaseCfg { url: string; key: string }

// 默认使用本地存储；云同步由使用者配置。
const LS_LOCAL_ONLY = 'sticky-todo-local-only'

export function loadCfg(): SupabaseCfg | null {
  try {
    if (localStorage.getItem(LS_LOCAL_ONLY) === '1') return null
    const raw = localStorage.getItem(LS_CFG)
    if (raw) return JSON.parse(raw)
    const url = import.meta.env.VITE_SUPABASE_URL
    const key = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY
    return url && key ? { url, key } : null
  } catch { return null }
}
export function saveCfg(cfg: SupabaseCfg | null) {
  if (cfg) {
    localStorage.setItem(LS_CFG, JSON.stringify(cfg))
    localStorage.removeItem(LS_LOCAL_ONLY)
  } else {
    localStorage.removeItem(LS_CFG)
    localStorage.setItem(LS_LOCAL_ONLY, '1')
  }
}

export const uid=()=>crypto.randomUUID()
export class TodoStore {
 todos:Todo[]=[];notes:Note[]=[];status:SyncStatus='connecting';userEmail:string|null=null
 visible=false;pending=0;revision=0;identity=0;private listeners=new Set<()=>void>()
 private ledger:Ledger|null=null;private login:Session|null=null;private config:SupabaseCfg|null=null
 private client:SupabaseClient|null=null;private authSub:{unsubscribe:()=>void}|null=null;private channel:ReturnType<SupabaseClient['channel']>|null=null
 private abort=new AbortController();private started=false;private fence='';private boot=0;private request=0;private syncing=false;private again=false;private timer:ReturnType<typeof setTimeout>|null=null
 private authBusy=false
 subscribe=(fn:()=>void)=>{this.listeners.add(fn);return ()=>{this.listeners.delete(fn)}}
 private emit(){this.revision++;this.listeners.forEach(f=>f())}
 get sorted(){return [...this.todos].sort((a,b)=>Number(b.pinned)-Number(a.pinned)||a.position-b.position)}
 get sortedNotes(){const key=this.ledger?.prefix+"note-order";const order:string[]=JSON.parse(localStorage.getItem(key)||"[]");const rank=new Map(order.map((id,i)=>[id,i]));return [...this.notes].sort((a,b)=>Number(b.pinned)-Number(a.pinned)||(rank.get(a.id)??-1)-(rank.get(b.id)??-1)||b.updated_at-a.updated_at||a.id.localeCompare(b.id))}
 moveNoteOnto(id:string,target:string){if(!this.allowed())return;const all=this.sortedNotes,i=all.findIndex(n=>n.id===id),j=all.findIndex(n=>n.id===target);if(i<0||j<0||i===j||all[i].pinned!==all[j].pinned)return;all.splice(j,0,all.splice(i,1)[0]);localStorage.setItem(this.ledger!.prefix+"note-order",JSON.stringify(all.map(n=>n.id)));this.emit()}
 private boundary(){return localStorage.getItem(ROOT+'access')||''}
 private allowed(){return !!this.ledger&&this.fence===this.boundary()&&(this.ledger.space==='local'?!loadCfg():!!this.login)}
 private clear(){this.identity++;this.request++;this.abort.abort();this.abort=new AbortController();this.login=null;this.ledger=null;this.userEmail=null;this.todos=[];this.notes=[];this.visible=false;this.pending=0;this.status='need-auth';this.syncing=false;this.again=false;if(this.timer)clearTimeout(this.timer);this.timer=null;const ch=this.channel;this.channel=null;if(ch&&this.client)void this.client.removeChannel(ch).catch(()=>{});this.emit()}
 draftKey(name:string){return this.allowed()?this.ledger!.prefix+'draft:'+name:null}
 get trash(){return this.allowed()?this.ledger!.trash():[]}
 restoreTrash(key:string){if(this.allowed()){this.ledger!.restore(key);this.show();this.schedule()}}
 private show(){if(!this.allowed()){this.clear();return}const b=this.ledger!.view();this.todos=b.todos as unknown as Todo[];this.notes=b.notes as unknown as Note[];this.pending=this.login?this.ledger!.ops().length:0;this.emit()}
 private access(tag:string){const stamp=tag+'|'+crypto.randomUUID();localStorage.setItem(ROOT+'access',stamp);return stamp}
 private async lock<T>(name:string,cb:()=>Promise<T>):Promise<T>{return navigator.locks.request(ROOT+name,cb)}
 async init(){if(this.started)return;this.started=true;const purge=()=>{for(let i=localStorage.length-1;i>=0;i--){const k=localStorage.key(i);if(!k?.startsWith(ROOT)||!k.includes(':bin:'))continue;try{const x=JSON.parse(localStorage.getItem(k)!);if(Number.isFinite(x.deleted)&&x.deleted+30*86400000<=Date.now())localStorage.removeItem(k)}catch{}}};purge();setInterval(()=>{purge();this.emit()},60000)
 window.addEventListener('storage',e=>{if(e.key===ROOT+'access'||e.key===LS_CFG||e.key===LS_LOCAL_ONLY||e.key===null){if(this.fence!==this.boundary()||e.key!==ROOT+'access'){this.clear();void this.prepare()}return}
 if(this.ledger&&e.key?.startsWith(this.ledger.prefix)&&!e.key.slice(this.ledger.prefix.length).startsWith('draft:')){this.show();if(e.key.includes(':op:')&&e.newValue)this.schedule()}})
 const focus=()=>{if(this.fence!==this.boundary()){this.clear();void this.prepare()}else if(this.login)this.schedule()};window.addEventListener('focus',focus);window.addEventListener('online',focus);document.addEventListener('visibilitychange',()=>{if(!document.hidden)focus()});setInterval(()=>{if(this.login&&!document.hidden)this.schedule()},30000)
 await this.prepare()}
 private async prepare(){const seq=++this.boot;this.clear();const cfg=loadCfg();const boundary=this.boundary();this.fence=boundary
 if(!cfg){this.authSub?.unsubscribe();this.authSub=null;this.client=null;this.config=null;this.ledger=new Ledger(localStorage,'local');this.ledger.migrate('',true);this.visible=true;this.status='local';this.show();return}
 if(!this.client||cfg.url!==this.config?.url||cfg.key!==this.config?.key){this.authSub?.unsubscribe();this.config=cfg;const client=this.client=createClient(cfg.url,cfg.key,{auth:{detectSessionInUrl:false}});this.authSub=client.auth.onAuthStateChange((event,session)=>{
 if(this.client!==client||this.authBusy||event==='INITIAL_SESSION')return
 if(event==='SIGNED_OUT'){this.boot++;this.clear();return}
 if(session?.user.id!==this.login?.user.id){this.clear();queueMicrotask(()=>{if(this.client===client&&!this.authBusy)void this.prepare()})}
 else if(session&&this.login){this.login=session;this.schedule()}
 }).data.subscription}
 if(boundary.startsWith('locked|'))return
 this.status='connecting';this.emit()
 try{const {data:{session},error}=await this.client!.auth.getSession();if(this.boot!==seq||this.boundary()!==boundary)return;if(error||!session){this.clear();return}this.activate(session,cfg,boundary)}catch{if(this.boot===seq)this.clear()}}
 private activate(session:Session,cfg:SupabaseCfg,boundary:string){const scope=spaceId(cfg.url,session.user.id)
 if(boundary&& !boundary.startsWith('user:'+encodeURIComponent(scope)+'|')){this.clear();return}
 this.login=session;this.fence=boundary;const L=this.ledger=new Ledger(localStorage,scope);this.userEmail=session.user.email??null;this.visible=true;this.status='connecting';this.show()
 const active=()=>this.ledger===L&&this.allowed();const sdk=this.client!
 const channel=this.channel=sdk.channel('sticky-v15-'+crypto.randomUUID());for(const table of ['todos','notes'])channel.on('postgres_changes',{event:'*',schema:'public',table},()=>{if(active())this.schedule()});channel.subscribe();this.schedule()
 }
 private async migrateLegacy(L:Ledger,session:Session,cfg:SupabaseCfg,active:()=>boolean){
  if(cfg.url!==String(import.meta.env.VITE_SUPABASE_URL||'')||localStorage.getItem(ROOT+'legacy-claim')||!session.user.email||localStorage.getItem('sticky-todo-owner')?.trim().toLowerCase()!==session.user.email.trim().toLowerCase())return
  // On a failed first launch offline this is retried at the next foreground sync.
  const {data,error}=await this.client!.auth.getUser(session.access_token)
  if(error||data.user?.id!==session.user.id||!data.user.email_confirmed_at||!active())return
  await this.lock('legacy-import',async()=>{if(active()){L.migrate(data.user!.email||'',false);this.show()}})
 }
 private schedule(){if(!this.login||!this.allowed()||this.timer)return;if(this.syncing){this.again=true;return}this.timer=setTimeout(()=>{this.timer=null;void this.flush()},70)}
 private async flush(){const L=this.ledger,session=this.login,cfg=this.config;if(!L||!session||!cfg||!this.allowed())return;const gen=this.request;const active=()=>gen===this.request&&this.ledger===L&&this.allowed();const signal=this.abort.signal;this.syncing=true
 try{await this.lock('sync:'+L.space,async()=>{if(!active())return;await this.migrateLegacy(L,session,cfg,active);if(!active())return;const api=new AccountApi({...cfg,token:session.access_token,user:session.user},signal);const ok=await reconcile(L,api,active);if(active()){this.status=ok?'online':'error';this.show()}})}catch{if(active()){this.status='error';this.show()}}finally{if(gen===this.request){this.syncing=false;if(this.again){this.again=false;this.schedule()}}}}
 async refresh(){if(this.login){this.schedule();return}await this.prepare()}
 async reconnect(cfg:SupabaseCfg){this.clear();this.access('locked');saveCfg(cfg);await this.prepare()}
 disconnect(){this.clear();this.access('locked');saveCfg(null);void this.prepare()}
 async enableCloud(){this.clear();localStorage.removeItem(LS_LOCAL_ONLY);this.access('locked');await this.prepare()}
 async signOut(){const sdk=this.client;this.boot++;this.access('locked');this.clear();this.fence=this.boundary()
 await this.lock('auth',async()=>{if(!this.boundary().startsWith('locked|'))return;this.authBusy=true;try{await sdk?.auth.signOut({scope:'local'})}catch{/* Local gate remains locked offline. */}finally{this.authBusy=false}})}
 private async auth(mode:'signInWithPassword'|'signUp',email:string,password:string):Promise<string|null>{
 return this.lock('auth',async()=>{if(!this.client)await this.prepare();const sdk=this.client;if(!sdk||!this.config)return '未配置云端'
 this.authBusy=true;this.boot++;this.access('locked');this.clear()
 try{const {data,error}=await sdk.auth[mode]({email,password});if(error)return error.message;if(!data.session)return '请先完成邮箱验证，再登录';const scope=spaceId(this.config.url,data.session.user.id);const mark=this.access('user:'+encodeURIComponent(scope));this.activate(data.session,this.config,mark);return null}
 catch{return '登录失败，请检查网络后重试'}finally{this.authBusy=false}})}
 signIn(email:string,password:string){return this.auth('signInWithPassword',email,password)}
 private async proofRequest(path:string,data:object,token?:string){const c=loadCfg();if(!c)throw Error("未配置同步");const r=await fetch(new URL("/auth/v1/"+path,c.url),{method:token?"PUT":"POST",redirect:"error",headers:{apikey:c.key,"Content-Type":"application/json",...(token?{Authorization:"Bearer "+token}:{})},body:JSON.stringify(data),signal:AbortSignal.timeout(15000)});const text=await r.text();let result:any={};try{result=text?JSON.parse(text):{}}catch{};if(!r.ok){const code=result.error_code||result.code;throw Object.assign(Error(r.status===429?"请求较频繁，请稍后再试。":path==="user"?(code==="same_password"?"邮箱已验证。这个密码就是原密码，请直接登录。":code==="weak_password"?"邮箱已验证，但新密码强度不足。请重新申请验证码并换一个更强的密码。":"邮箱已验证，但密码没有修改成功。请重试找回流程。"):path==="verify"?"验证码错误或已失效，请核对最新邮件。":"未能发送邮件，请稍后再试。"),{code})}return result}
 async beginSignup(email:string){await this.proofRequest("otp",{email,create_user:true})}
 async beginRecovery(email:string){await this.proofRequest("recover",{email})}
 async confirmSignup(email:string,password:string,proof:string,recovery=false){const c=loadCfg();if(!c)throw Error("未配置同步");let payload:object;if(/^[0-9]{6,10}$/.test(proof))payload={type:recovery?"recovery":"email",email,token:proof};else{let u=new URL(/^(www\.)?google\.com\/url\?/.test(proof)?"https://"+proof:proof);if(u.protocol==="https:"&&["google.com","www.google.com"].includes(u.hostname)&&u.pathname==="/url")u=new URL(u.searchParams.get("q")||u.searchParams.get("url")||"");const type=u.searchParams.get("type"),token=u.searchParams.get("token")||u.searchParams.get("token_hash");if(u.origin!==new URL(c.url).origin||u.username||u.password||u.pathname!=="/auth/v1/verify"||!(recovery?["recovery"]:["signup","email","magiclink"]).includes(type||"")||!token)throw Error("请使用本次操作的最新邮件验证码或原始链接");payload={type,token_hash:token}}const x=await this.proofRequest("verify",payload);if(!x.access_token||x.user?.email?.toLowerCase()!==email.toLowerCase())throw Error("验证邮箱不匹配");await this.proofRequest("user",{password},x.access_token);return recovery?null:this.signIn(email,password)}
 private edit(k:Kind,type:Op['type'],r:Row,patch:Partial<Row>={}){if(!this.allowed())return;try{if(type==='delete')this.ledger!.recycle(k,r);else this.ledger!.edit(k,type,r,patch);this.show();this.schedule()}catch{window.alert('未能保存到本机，请保留输入并检查磁盘空间。');throw Error('本机保存失败')}}
 add(text:string){const now=Date.now();this.edit('todos','add',{id:uid(),text,done:false,pinned:false,position:Math.max(-1,...this.todos.filter(t=>!t.done).map(t=>t.position))+1,created_at:now,completed_at:null})}
 update(id:string,patch:Partial<Todo>){const e=this.todos.find(t=>t.id===id);if(!e)return;const p={...patch};if(p.done!==undefined&&p.done!==e.done)p.completed_at=p.done?Date.now():null;this.edit('todos','patch',{...e,...p},p)}
 remove(id:string){const e=this.todos.find(t=>t.id===id);if(e)this.edit('todos','delete',{...e})}
 moveOnto(id:string,target:string){const a=this.peers(id),i=a.findIndex(t=>t.id===id),j=a.findIndex(t=>t.id===target);if(i<0||j<0||i===j)return;a.splice(j,0,a.splice(i,1)[0]);a.forEach((r,n)=>{if(r.position!==n)this.update(r.id,{position:n})})}
 private peers(id:string){const e=this.todos.find(r=>r.id===id);return this.sorted.filter(r=>!r.done&&r.pinned===e?.pinned)}
 moveLimit(id:string,d:number){const a=this.peers(id),i=a.findIndex(r=>r.id===id);return i<0||!a[i+d]}
 move(id:string,d:number){const a=this.peers(id),i=a.findIndex(r=>r.id===id);if(i<0||!a[i+d])return;[a[i],a[i+d]]=[a[i+d],a[i]];a.forEach((r,n)=>{if(r.position!==n)this.update(r.id,{position:n})})}
 reorder(id:string,before:string|null){const e=this.todos.find(t=>t.id===id);if(!e)return;const a=this.sorted.filter(t=>t.id!==id);const i=a.findIndex(t=>t.id===before);a.splice(i<0?a.length:i,0,e);a.forEach((r,n)=>{if(r.position!==n)this.update(r.id,{position:n})})}
 addNote(title:string,content:string){const id=uid(),now=Date.now();this.edit('notes','add',{id,title,content,pinned:false,updated_at:now,created_at:now});return id}
 updateNote(id:string,patch:Partial<Note>){const e=this.notes.find(n=>n.id===id);if(!e)return;const p={...patch,updated_at:Date.now()};this.edit('notes','patch',{...e,...p},p)}
 removeNote(id:string){const e=this.notes.find(n=>n.id===id);if(e)this.edit('notes','delete',{...e})}
}
export const store=new TodoStore()
