package com.gray.sticky.nativeapp
import org.json.JSONObject
internal object Wire {
 val fields=mapOf("todos" to listOf("id","text","done","pinned","position","created_at","completed_at"),"notes" to listOf("id","title","content","pinned","created_at","updated_at"))
 fun from(s:String,k:String,o:JSONObject)=Entry().apply {space=s;kind=k;id=o.getString("id");pinned=o.getBoolean("pinned");created=o.getLong("created_at")
  if(k=="todos"){title=o.getString("text");done=o.getBoolean("done");position=o.getDouble("position");completed=if(o.isNull("completed_at"))null else o.getLong("completed_at");updated=completed?:created}
  else {title=o.getString("title");body=o.getString("content");updated=o.getLong("updated_at")}}
 fun json(e:Entry)=JSONObject().apply {put("id",e.id);put("pinned",e.pinned);put("created_at",e.created)
  if(e.kind=="todos"){put("text",e.title);put("done",e.done);put("position",e.position);put("completed_at",e.completed?:JSONObject.NULL)}
  else {put("title",e.title);put("content",e.body);put("updated_at",e.updated)}}
 fun canon(e:Entry)=json(e).toString()
}