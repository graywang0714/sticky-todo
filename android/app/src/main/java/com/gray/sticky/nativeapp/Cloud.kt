package com.gray.sticky.nativeapp

import android.content.Context
import androidx.lifecycle.MutableLiveData
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.*
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

internal data class CloudView(val space:String="local",val email:String="",val status:String="仅本机",val needLogin:Boolean=false)
internal class Cloud private constructor(ctx:Context) {
 companion object {@Volatile private var shared:Cloud?=null; @Synchronized fun open(c:Context)=shared?:Cloud(c.applicationContext).also{shared=it}}
 private val db=JournalDb.open(ctx);private val dao=db.entries();private val vault=Vault(ctx)
 private val worker=ScheduledThreadPoolExecutor(1).apply{removeOnCancelPolicy=true}
 private val api=runCatching{val d=ctx.assets.open("client.json").bufferedReader().use{JSONObject(it.readText())};val u=d.getString("url").toHttpUrl();require(u.isHttps && u.host.endsWith(".supabase.co"));CloudApi(u,d.getString("key"))}.getOrNull()
 val authResult=MutableLiveData(AuthResult());private val authGate=AtomicBoolean(false);private var counter=0L
 private var reauth=false;private var recovery:JSONObject?=null;private var recoveryMail="";private var recoveryUntil=0L;private var recoveryProofId=""
 val view=MutableLiveData(CloudView());private var session:JSONObject?=null
 @Volatile private var active=false;private val pending=AtomicBoolean(false);private var sock:WebSocket?=null;private var socketToken="";private var beat=0;private var verified=false;private var lastHeartbeat=0L
 init {worker.execute{try{session=vault.load();state(if(session!=null)"待同步" else "仅本机");kick()}catch(_:Exception){state("登录状态不可用，请重新登录")}}
 worker.scheduleWithFixedDelay({if(active && session!=null)kick()},25,25,TimeUnit.SECONDS)}
 private fun uid()=session?.getJSONObject("user")?.getString("id")?:"local"
 private fun state(s:String){view.postValue(CloudView(uid(),session?.getJSONObject("user")?.optString("email")?:"",s,reauth))}
 fun activate(on:Boolean){active=on;worker.execute{if(on)kick()else closeSocket()}}
 private fun closeSocket(){val old=sock;sock=null;verified=false;old?.cancel()}
 private fun save(s:JSONObject){UUID.fromString(s.getJSONObject("user").getString("id"));require(s.getString("access_token").isNotBlank()&&s.getString("refresh_token").isNotBlank())
  if(!s.has("expires_at"))s.put("expires_at",System.currentTimeMillis()/1000+s.optLong("expires_in",3600));vault.save(s);session=s;reauth=false}
 fun accountAction(mode:String,email:String,password:String,proof:String){
  if(!authGate.compareAndSet(false,true))return
  authResult.postValue(AuthResult(busy=true));worker.execute{val seq=++counter
   fun done(tag:String,info:String,fail:Boolean=false){authResult.postValue(AuthResult(false,tag,info,fail,email,seq))}
   fun enter(x:JSONObject){recovery=null;save(x);closeSocket();state("正在同步");kick();done("session","已登录")}
   try{val a=api?:error("config")
    when(mode){
     "login"->enter(a.token("password",JSONObject().put("email",email).put("password",password)))
     "register"->{a.auth("otp",JSONObject().put("email",email).put("create_user",true));done("confirmation","验证邮件已发送。请使用邮件验证码或复制验证链接，再设置密码。")}
     "confirm"->{val verified=a.auth("verify",AuthFlow.proof(a.base,proof,email,"email"));require(verified.getJSONObject("user").getString("email").equals(email,true));a.setPassword(verified.getString("access_token"),password);enter(verified)}
     "recover"->{recovery=null;a.auth("recover",JSONObject().put("email",email));done("sent","若该邮箱对应有效账号，你将收到恢复邮件。")}
     "reset"->{val proofId=java.security.MessageDigest.getInstance("SHA-256").digest(proof.trim().toByteArray()).joinToString(""){"%02x".format(it)};val valid=recoveryProofId==proofId&&recoveryMail.equals(email,true)&&System.currentTimeMillis()<recoveryUntil
      val verified=(if(valid)recovery else null)?:a.auth("verify",AuthFlow.proof(a.base,proof,email)).also{require(it.getJSONObject("user").getString("email").equals(email,true));recovery=it;recoveryProofId=proofId;recoveryMail=email;recoveryUntil=System.currentTimeMillis()+180000}
      a.setPassword(verified.getString("access_token"),password);recovery=null;done("changed","密码已更新，请使用新密码登录。")}
     else->done("error","不支持此操作",true)
    }
   }catch(e:Exception){if(e is HttpFailure&&e.reason=="same_password"){recovery=null;done("changed","邮箱已验证。刚才填写的是原密码，请直接登录。")}else done("error",AuthFlow.message(e),true)}finally{authGate.set(false)}
  }
 }
 fun logout(){worker.execute{val token=session?.optString("access_token");closeSocket();vault.save(null);session=null;reauth=false;recovery=null;state("仅本机");if(!token.isNullOrEmpty())runCatching{api?.revoke(token)}}}
 fun kick(){if(!active||!pending.compareAndSet(false,true))return;worker.execute{pending.set(false);if(!active||session==null||reauth)return@execute
 try{sync()}catch(e:Exception){if(e is HttpFailure && e.reason in setOf("refresh_token_not_found","refresh_token_already_used","session_not_found","user_not_found") ){reauth=true;closeSocket()};if(e is HttpFailure && e.status==401){session?.put("expires_at",0)}
 state(if(e is HttpFailure && e.status in listOf(400,401,403))"账号或权限需核对；修改已留在本机" else "暂未同步 · 修改已保存在手机")}}}
 private fun sync(){val a=api?:error("config");var s=session?:return;val uid=uid()
  if(s.optLong("expires_at")<System.currentTimeMillis()/1000+60){val n=a.token("refresh_token",JSONObject().put("refresh_token",s.getString("refresh_token")));check(n.getJSONObject("user").getString("id")==uid);save(n);s=n}
  val t=s.getString("access_token");ensureSocket(a,uid,t)
  var retry=false;var blocked=false
  for(k in listOf("todos","notes")){
   if(!active)return
   val remote=a.list(uid,t,k)
   val ids=dao.rows(uid,k).map{it.id}.toSet()+remote.keys
   for(id in ids){val r=remote[id];val remoteCanon=r?.let(Wire::canon)
    var send:Entry?=null
    db.runInTransaction{
     val l=dao.one(uid,k,id)
     fun adopt(){if(r==null)dao.remove(uid,k,id)else{r.shadow=remoteCanon;r.dirty=false;dao.put(r)}}
     if(l==null){if(r!=null)adopt()}else if(!l.dirty){if(l.shadow!=remoteCanon || r==null)adopt()}else{
      val local=if(l.hidden)null else Wire.canon(l)
      when(Reconcile.action(l.shadow,local,remoteCanon)){
       "ACK","ADOPT"->adopt()
       "SEND"->send=l
       "CONFLICT"->{val copy=Wire.from(uid,"notes",JSONObject().put("id",UUID.randomUUID().toString()).put("title","冲突备份："+l.title.take(60)).put("content",(if(l.hidden)"本机曾请求删除。\n" else "")+l.title+"\n"+l.body).put("pinned",true).put("created_at",System.currentTimeMillis()).put("updated_at",System.currentTimeMillis()));copy.space="backup:"+uid;dao.put(copy);adopt()}
      }
     }
    }
    val sent=send?:continue
    val ok=try{a.mutate(uid,t,sent)}catch(e:HttpFailure){if(e.status==409)false else if(e.status in listOf(400,403,413,414,422)){blocked=true;continue}else throw e}
    if(!ok){retry=true;continue}
    val sentCanon=if(sent.hidden)null else Wire.canon(sent)
    db.runInTransaction{val now=dao.one(uid,k,id)?:return@runInTransaction
     if(now.hidden&&sent.hidden){dao.remove(uid,k,id)}else{now.shadow=sentCanon;now.dirty=(if(now.hidden)null else Wire.canon(now))!=sentCanon;dao.put(now)}}
   }
  }
  state(if(blocked)"有修改未上传 · 请检查权限或内容长度" else if(retry)"正在核对变更" else if(verified)"实时同步已连接" else "已核对 · 正在连接实时通道")
  if(retry)worker.schedule({kick()},950,TimeUnit.MILLISECONDS)
 }
 private fun ensureSocket(a:CloudApi,uid:String,t:String){val now=System.currentTimeMillis()
  if(sock!=null&&socketToken==t){if(lastHeartbeat!=0L&&now-lastHeartbeat>60000){closeSocket()}else{sock?.send(JSONObject().put("topic","phoenix").put("event","heartbeat").put("payload",JSONObject()).put("ref","h"+(++beat)).toString());return}}
  closeSocket();socketToken=t;lastHeartbeat=now
  val url=a.base.newBuilder().addPathSegments("realtime/v1/websocket").addQueryParameter("apikey",a.key).addQueryParameter("vsn","1.0.0").build()
  val topic="realtime:sticky-native-"+uid
  sock=a.http.newWebSocket(Request.Builder().url(url).build(),object:WebSocketListener(){
   override fun onOpen(ws:WebSocket,r:Response){val changes=JSONArray();for(k in listOf("todos","notes"))changes.put(JSONObject().put("event","*").put("schema","public").put("table",k))
    val cfg=JSONObject().put("broadcast",JSONObject().put("self",false)).put("presence",JSONObject().put("key","")).put("postgres_changes",changes)
    ws.send(JSONObject().put("topic",topic).put("event","phx_join").put("ref","1").put("join_ref","1").put("payload",JSONObject().put("config",cfg).put("access_token",t)).toString())}
   override fun onMessage(ws:WebSocket,text:String){val x=runCatching{JSONObject(text)}.getOrNull()?:return
    worker.execute{if(sock!==ws)return@execute;lastHeartbeat=System.currentTimeMillis()
     when(x.optString("event")){"phx_reply"->if(x.optString("ref")=="1"){val pl=x.optJSONObject("payload");val reg=pl?.optJSONObject("response")?.optJSONArray("postgres_changes");verified=pl?.optString("status")=="ok" && reg!=null && (0 until reg.length()).mapNotNull{reg.optJSONObject(it)?.optString("table")}.containsAll(listOf("todos","notes"));if(verified)kick()else closeSocket()};"postgres_changes"->kick();"system"->if(x.optJSONObject("payload")?.optString("status")=="error"){closeSocket();state("实时订阅异常 · 已启用定时核对")};"phx_error","phx_close"->closeSocket()}}}
   override fun onFailure(ws:WebSocket,t:Throwable,r:Response?){worker.execute{if(sock===ws){closeSocket();state("实时连接中断 · 将自动重连")}}}
   override fun onClosed(ws:WebSocket,code:Int,reason:String){worker.execute{if(sock===ws)closeSocket()}}
  })
 }
}