package com.gray.sticky.nativeapp

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.*
import java.io.IOException
import java.util.concurrent.TimeUnit
internal class HttpFailure(val status:Int,val reason:String=""):IOException("HTTP "+status)
internal class CloudApi(val base:HttpUrl,val key:String,val http:OkHttpClient=OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).connectTimeout(5,TimeUnit.SECONDS).readTimeout(8,TimeUnit.SECONDS).callTimeout(12,TimeUnit.SECONDS).build()){
 private val json="application/json".toMediaType()
 fun token(grant:String,input:JSONObject):JSONObject{
  val url=base.newBuilder().addPathSegments("auth/v1/token").addQueryParameter("grant_type",grant).build()
  return JSONObject(request(url,"POST",null,input))
 }
 fun auth(endpoint:String,input:JSONObject):JSONObject {require(endpoint in setOf("signup","recover","verify","otp"));val text=request(base.newBuilder().addPathSegments("auth/v1/"+endpoint).build(),"POST",null,input);return if(text.isBlank())JSONObject() else JSONObject(text)}
 fun setPassword(token:String,pw:String){request(base.newBuilder().addPathSegments("auth/v1/user").build(),"PUT",token,JSONObject().put("password",pw))}
 fun revoke(token:String){request(base.newBuilder().addPathSegments("auth/v1/logout").addQueryParameter("scope","local").build(),"POST",token,JSONObject())}
 private fun request(url:HttpUrl,m:String,t:String?,o:JSONObject?=null,representation:Boolean=false):String{
  val b=Request.Builder().url(url).header("apikey",key);if(t!=null)b.header("Authorization","Bearer "+t)
  if(representation)b.header("Prefer","return=representation")
  val req=b.method(m,o?.toString()?.toRequestBody(json)).build()
  http.newCall(req).execute().use{if(!it.isSuccessful){val reason=runCatching{JSONObject(it.peekBody(2048).string()).optString("error_code")}.getOrDefault("");throw HttpFailure(it.code,reason)};return it.body?.string()?:""}
 }
 fun list(s:String,t:String,kind:String):Map<String,Entry>{
  val fields=Wire.fields.getValue(kind);val out=linkedMapOf<String,Entry>();var after:String?=null
  while(true){val b=base.newBuilder().addPathSegments("rest/v1/"+kind).addQueryParameter("select",(fields+"user_id").joinToString(",")).addQueryParameter("user_id","eq."+s).addQueryParameter("order","id.asc").addQueryParameter("limit","200")
   if(after!=null)b.addQueryParameter("id","gt."+after);val a=JSONArray(request(b.build(),"GET",t))
   for(i in 0 until a.length()){val o=a.getJSONObject(i);check(o.getString("user_id")==s);val e=Wire.from(s,kind,o);check(!out.containsKey(e.id));out[e.id]=e;after=e.id}
   if(a.length()<200)return out
  }
 }
 fun mutate(uid:String,t:String,e:Entry):Boolean {
  val b=base.newBuilder().addPathSegments("rest/v1/"+e.kind)
  val prev=e.shadow?.let(::JSONObject);val method=if(e.hidden)"DELETE" else if(prev==null)"POST" else "PATCH"
  if(method!="POST"){
   b.addQueryParameter("id","eq."+e.id).addQueryParameter("user_id","eq."+uid)
   if(prev!=null)for(f in (if(e.kind=="notes")listOf("updated_at","created_at","pinned") else Wire.fields.getValue(e.kind).filter{it!="id"})){val v=prev.opt(f);b.addQueryParameter(f,when(v){null,JSONObject.NULL->"is.null";is String->"eq."+v;else->"eq."+v.toString()})}
  }
  val body=if(e.hidden)null else Wire.json(e).apply{if(method=="POST")put("user_id",uid) else remove("id")}
  val a=JSONArray(request(b.build(),method,t,body,true));return a.length()==1
 }
}