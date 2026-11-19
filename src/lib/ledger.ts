// No auth tokens in these records. Each edit has its own durable key so two
// windows cannot replace one another's pending work with an old array.
export type Kind='todos'|'notes'
export type Row={id:string;[prop:string]:any}
export type Book=Record<Kind,Row[]>
export type Op={key:string;seq:number;kind:Kind;type:'add'|'patch'|'delete'|'legacy';row:Row;patch:Partial<Row>;copyId:string}
export const ROOT='sticky:v15:'
export const spaceId=(origin:string,uid:string)=>JSON.stringify([new URL(origin).origin,uid])
export const blank=():Book=>({todos:[],notes:[]})
export const keys:Record<Kind,string[]>={todos:['id','text','done','pinned','position','created_at','completed_at'],notes:['id','title','content','pinned','created_at','updated_at']}
export const clean=(k:Kind,r:Row):Row=>Object.fromEntries(keys[k].filter(n=>n in r).map(n=>[n,r[n]])) as Row
export const same=(k:Kind,a:Row,b:Row)=>keys[k].every(f=>(a[f]??null)===(b[f]??null))
function replace(rows:Row[],id:string,row:Row|null){const i=rows.findIndex(r=>r.id===id);if(row){if(i<0)rows.push(row);else rows[i]=row}else if(i>=0)rows.splice(i,1)}
export class Ledger{
 readonly prefix:string
 constructor(privateStorage:Storage,readonlySpace:string){this.s=privateStorage;this.space=readonlySpace;this.prefix=ROOT+encodeURIComponent(this.space)+':'}
 private s:Storage;readonly space:string
 base():Book{const v=this.s.getItem(this.prefix+'base');return v?JSON.parse(v):blank()}
 snapshot(b:Book){this.s.setItem(this.prefix+'base',JSON.stringify(b))}
 ops():Op[]{const p=this.prefix+'op:';const v:Op[]=[];for(let i=0;i<this.s.length;i++){const key=this.s.key(i);if(key?.startsWith(p)){const raw=this.s.getItem(key);if(raw)v.push(JSON.parse(raw))}}return v.sort((a,b)=>a.seq-b.seq||a.key.localeCompare(b.key))}
 view():Book{const b=this.base();for(const o of this.ops()){const rows=b[o.kind],prev=rows.find(x=>x.id===o.row.id);replace(rows,o.row.id,o.type==='delete'?null:o.type==='patch'?{...(prev||o.row),...o.patch}:o.row)}return b}
 edit(k:Kind,type:Op['type'],r:Row,patch:Partial<Row>={},fixed?:string):Op{const id=fixed||crypto.randomUUID();const p=this.prefix+'op:'+id;const prior=fixed?this.s.getItem(p):null;if(prior)return JSON.parse(prior);const all=this.ops();const seq=fixed?0:Math.max(Date.now(),(all.at(-1)?.seq||0)+1);const o:Op={key:p,seq,kind:k,type,row:clean(k,r),patch,copyId:crypto.randomUUID()};this.s.setItem(p,JSON.stringify(o));return o}
 ack(o:Op,returned:Row|null,other?:Row){const b=this.base();replace(b[o.kind],o.row.id,returned);if(other)replace(b[o.kind],other.id,other);this.snapshot(b);this.s.removeItem(o.key)}
 trash(now=Date.now()){const items:{key:string;kind:Kind;row:Row;deleted:number}[]=[];for(let i=this.s.length-1;i>=0;i--){const k=this.s.key(i);if(!k?.startsWith(this.prefix+'bin:'))continue;const x=JSON.parse(this.s.getItem(k)!);if(x.deleted+30*86400000<=now){this.s.removeItem(k);continue}items.push({...x,key:k})}return items.sort((a,b)=>b.deleted-a.deleted)}
 recycle(k:Kind,r:Row){this.s.setItem(this.prefix+'bin:'+crypto.randomUUID(),JSON.stringify({kind:k,row:clean(k,r),deleted:Date.now(),restoreId:crypto.randomUUID()}));this.edit(k,'delete',r)}
 restore(key:string){if(!key.startsWith(this.prefix+'bin:'))return;const raw=this.s.getItem(key);if(!raw)return;const x=JSON.parse(raw);if(x.deleted+30*86400000>Date.now())this.edit(x.kind,'add',{...x.row,id:x.restoreId});this.s.removeItem(key)}
 migrate(email:string,local:boolean){const owner=this.s.getItem('sticky-todo-owner')?.trim().toLowerCase()||'';const mark=ROOT+'legacy-claim';if(this.s.getItem(mark))return;
 if(local?(owner!==''||this.s.getItem('sticky-todo-local-only')!=='1'):(!email||owner!==email.trim().toLowerCase()))return;
 const src:Book=blank();for(const k of ['todos','notes'] as const){const raw=this.s.getItem(k==='todos'?'sticky-todo-items':'sticky-todo-notes');if(raw){src[k]=JSON.parse(raw);if(!Array.isArray(src[k])||src[k].some(r=>!r||typeof r.id!=='string'))throw Error('旧缓存格式异常，原件已保留')}}
 if(!src.todos.length&&!src.notes.length)return;
 for(const k of ['todos','notes'] as const)for(const r of src[k]){if(r.user_id&&(local||r.user_id!==JSON.parse(this.space)[1]))continue;this.edit(k,'legacy',r,{},'old:'+k+':'+r.id)}
 this.s.setItem(mark,this.space) // Last: interrupted import retries idempotently. Never remove original keys.
 }
}
