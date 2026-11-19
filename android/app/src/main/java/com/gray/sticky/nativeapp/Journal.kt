package com.gray.sticky.nativeapp

import java.util.UUID
import java.util.concurrent.Executors
import org.json.JSONObject

class Journal(private val db:JournalDb,val space:String="local",private val changed:()->Unit={}){
 companion object{val io=Executors.newSingleThreadExecutor()}
 private val dao=db.entries()
 private val online get()=space!="local"&&!space.startsWith("backup:")
 fun watch()=dao.watch(space)
 private fun transaction(f:()->Unit){io.execute{db.runInTransaction{f()};changed()}}
 private fun touch(e:Entry){e.updated=maxOf(e.updated+1,System.currentTimeMillis());e.dirty=online;dao.put(e)}
 fun create(kind:String,title:String,body:String=""){val name=EntryPolicy.text(title)?:if(kind=="notes")EntryPolicy.text(body)?.lineSequence()?.first()?.take(40)else null
  if(name==null)return;transaction{val now=System.currentTimeMillis();val e=Entry().also{it.id=UUID.randomUUID().toString();it.space=space;it.kind=kind;it.title=name;it.body=body;it.created=now;it.position=(dao.rows(space,kind).maxOfOrNull{r->r.position}?:-1.0)+1};touch(e)}}
 private fun change(k:String,id:String,fn:(Entry)->Unit){transaction{dao.one(space,k,id)?.takeUnless{it.hidden}?.let{fn(it);touch(it)}}}
 private fun preserve(t:String,b:String){val now=System.currentTimeMillis();val copy=Entry().also{it.id=UUID.randomUUID().toString();it.kind="notes";it.space=if(online)"backup:"+space else space;it.title="冲突备份："+t.take(60);it.body=t+"\n"+b;it.pinned=true;it.created=now;it.updated=now};dao.put(copy)}
 fun save(k:String,id:String,t:String,b:String,oldTitle:String,oldBody:String){val name=EntryPolicy.text(t)?:EntryPolicy.text(b)?.lineSequence()?.first()?.take(40)?:return
  transaction{val e=dao.one(space,k,id);if(e==null||e.hidden){preserve(name,b)}else{val title=Draft.merge(oldTitle,name,e.title);val body=Draft.merge(oldBody,b,e.body);if(title==null||body==null)preserve(name,b)else if(title!=e.title||body!=e.body){e.title=title;e.body=body;touch(e)}}}}
 fun pin(k:String,id:String)=change(k,id){it.pinned=!it.pinned}
 fun check(id:String)=change("todos",id){it.done=!it.done;it.completed=EntryPolicy.completion(it.done,System.currentTimeMillis())}
 fun remove(k:String,id:String,seen:String){transaction{val e=dao.one(space,k,id)?:return@transaction;if(Wire.canon(e)!=seen){preserve("删除请求："+e.title,"记录已变更，未自动删除。");return@transaction};val now=System.currentTimeMillis();val bin=Entry().also{it.id=UUID.randomUUID().toString();it.space="trash:"+space;it.kind=k;it.title=e.title;it.body=Wire.canon(e);it.created=now;it.updated=now};dao.put(bin);if(!online)dao.remove(space,k,id)else{e.hidden=true;touch(e)}}}
 fun bin()=dao.watch("trash:"+space)
 fun expire(){io.execute{dao.expire(System.currentTimeMillis()-30L*86400000)}}
 fun restore(k:String,id:String){transaction{val b=dao.one("trash:"+space,k,id)?:return@transaction;if(b.updated+30L*86400000>System.currentTimeMillis()){val e=Wire.from(space,k,JSONObject(b.body));e.id=UUID.randomUUID().toString();touch(e)};dao.remove(b.space,k,id)}}
 fun watchNoteOrder()=dao.watch("order:"+space)
 fun orderedNotes(rows:List<Entry>,record:Entry?):List<Entry>{val rank=HashMap<String,Int>();if(record!=null){val a=org.json.JSONArray(record.body);for(i in 0 until a.length())rank[a.getString(i)]=i};return rows.sortedWith(compareByDescending<Entry>{it.pinned}.thenBy{rank[it.id]?:-1}.thenByDescending{it.updated}.thenBy{it.id})}
 fun moveNoteOnto(id:String,target:String){io.execute{db.runInTransaction{val key="order:"+space;val list=orderedNotes(dao.rows(space,"notes").filterNot{it.hidden},dao.one(key,"order","notes")).toMutableList();val i=list.indexOfFirst{it.id==id};val to=list.indexOfFirst{it.id==target};if(i<0||to<0||i==to||list[i].pinned!=list[to].pinned)return@runInTransaction;list.add(to,list.removeAt(i));dao.put(Entry().also{it.space=key;it.kind="order";it.id="notes";it.body=org.json.JSONArray(list.map{n->n.id}).toString()})}}}
 fun moveOnto(id:String,target:String){transaction{val e=dao.one(space,"todos",id)?:return@transaction;val a=dao.rows(space,"todos").filter{!it.hidden&&!it.done&&it.pinned==e.pinned}.toMutableList();val i=a.indexOfFirst{it.id==id};if(i<0)return@transaction;val dest=a.indexOfFirst{it.id==target};if(dest<0||i==dest)return@transaction;a.add(dest,a.removeAt(i));a.forEachIndexed{j,x->if(x.position!=j.toDouble()){x.position=j.toDouble();touch(x)}}}}
}