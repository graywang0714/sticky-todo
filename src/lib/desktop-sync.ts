import {blank,clean,keys,same} from './ledger'
import type {Book,Kind,Ledger,Op,Row} from './ledger'
export type SessionUser={id:string;email?:string;email_confirmed_at?:string}
export type Credentials={url:string;key:string;token:string;user:SessionUser}
const kinds=['todos','notes'] as const
export class AccountApi{
 private c:Credentials;private signal:AbortSignal
 constructor(c:Credentials,signal:AbortSignal){this.c=c;this.signal=signal}
 private url(k:Kind){const u=new URL('/rest/v1/'+k,this.c.url);u.searchParams.set('select',keys[k].join(',')+',user_id');return u}
 private async request(u:URL,method='GET',data?:unknown):Promise<Row[]>{
 const res=await fetch(u,{method,redirect:'error',cache:'no-store',credentials:'omit',signal:AbortSignal.any([this.signal,AbortSignal.timeout(12000)]),headers:{apikey:this.c.key,Authorization:'Bearer '+this.c.token,'Content-Type':'application/json',Prefer:'return=representation'},body:data===undefined?undefined:JSON.stringify(data)})
 if(!res.ok)throw Object.assign(Error('同步返回 '+res.status),{status:res.status})
 const rows=await res.json();if(!Array.isArray(rows)||rows.some(r=>r.user_id!==this.c.user.id||typeof r.id!=='string'))throw Error('返回的记录归属不符')
 return rows}
 async all(k:Kind){const a:Row[]=[];let last='';for(;;){const u=this.url(k);u.searchParams.set('user_id','eq.'+this.c.user.id);u.searchParams.set('order','id.asc');u.searchParams.set('limit','200');if(last)u.searchParams.set('id','gt.'+last);const x=await this.request(u);a.push(...x.map(r=>clean(k,r)));if(x.length<200)return a;const n=x[x.length-1].id;if(n===last)throw Error('游标未推进');last=n}}
 async add(k:Kind,r:Row){const x=await this.request(this.url(k),'POST',{...clean(k,r),user_id:this.c.user.id});if(x.length!==1)throw Error('写入未获确认');return clean(k,x[0])}
 async change(k:Kind,id:string,patch:Partial<Row>|null){const u=this.url(k);u.searchParams.set('id','eq.'+id);u.searchParams.set('user_id','eq.'+this.c.user.id);const x=await this.request(u,patch?'PATCH':'DELETE',patch||undefined);if(patch&&x.length!==1)throw Error('远端记录已变化，本机编辑已保留');return x.length?clean(k,x[0]):null}
}
// Caller holds a cross-window lock for this account. Never acknowledges work
// issued for a previous login generation. A later edit has its own journal key.
export async function reconcile(L:Ledger,api:Pick<AccountApi,'all'|'add'|'change'>,active:()=>boolean){
 const remote:Book=blank();[remote.todos,remote.notes]=await Promise.all(kinds.map(k=>api.all(k)))
 if(!active())return false;L.snapshot(remote);let success=true;const blocked=new Set<string>()
 for(const op of L.ops()){if(!active())return false;const key=op.kind+'/'+op.row.id;if(blocked.has(key))continue;const list=remote[op.kind];const prev=list.find(r=>r.id===op.row.id);let value:Row|null=null,extra:Row|undefined
 try{if(op.type==='delete'){if(prev)await api.change(op.kind,op.row.id,null)}
 else if(op.type==='add'||op.type==='legacy'){if(!prev)value=await api.add(op.kind,op.row);else if(same(op.kind,prev,op.row))value=prev;else{value=prev;extra=await copy(op)}}
 else if(!prev){extra=await copy(op)}else{value=Object.entries(op.patch).every(([k,v])=>prev[k]===v)?prev:await api.change(op.kind,op.row.id,op.patch)}
 if(!active())return false;L.ack(op,value,extra);remote[op.kind]=L.base()[op.kind]
 }catch{if(!active())return false;blocked.add(key);success=false}
 }
 return success
 async function copy(o:Op){const label=o.kind==='todos'?'text':'title';const r={...o.row,id:o.copyId,[label]:'保留副本：'+o.row[label]};const already=remote[o.kind].find(i=>i.id===r.id);if(already){if(!same(o.kind,already,r))throw Error('副本冲突');return already}return api.add(o.kind,r)}
}
