package com.gray.sticky.nativeapp

import org.json.JSONObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
internal object AuthFlow {
 fun email(s:String)=s.length<=254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(s.trim())
 fun proof(base:HttpUrl,input:String,email:String,purpose:String="recovery"):JSONObject{
  require(purpose in setOf("recovery","email"));val raw=input.trim();require(raw.length<=8192);if(raw.matches(Regex("[0-9]{6,10}")))return JSONObject().put("type",purpose).put("email",email.trim()).put("token",raw)
  val outer=(if(raw.startsWith("google.com/url?")||raw.startsWith("www.google.com/url?")) "https://"+raw else raw).toHttpUrlOrNull()?:throw IllegalArgumentException()
  val u=if(outer.isHttps&&outer.port==443&&outer.username.isEmpty()&&outer.password.isEmpty()&&outer.host in setOf("google.com","www.google.com")&&outer.encodedPath=="/url"){(outer.queryParameter("q")?:outer.queryParameter("url"))?.toHttpUrlOrNull()?:throw IllegalArgumentException()}else outer
  val linkType=u.queryParameter("type")
  require(u.isHttps&&u.host==base.host&&u.port==base.port&&u.username.isEmpty()&&u.password.isEmpty()&&u.encodedPath=="/auth/v1/verify"&&(if(purpose=="recovery")linkType=="recovery" else linkType in setOf("email","signup","magiclink")))
  val token=u.queryParameter("token")?:u.queryParameter("token_hash")?:throw IllegalArgumentException()
  require(token.matches(Regex("[A-Za-z0-9_.~-]{16,256}")));return JSONObject().put("token_hash",token).put("type",if(purpose=="email")linkType else purpose)
 }
 fun message(x:Exception)=when{
  x is IllegalArgumentException->"请填写邮件中的验证码，或复制本项目的原始恢复链接。"
  x is HttpFailure&&x.status==429->"请求较频繁，请稍后再试。"
  x is HttpFailure&&x.reason=="email_not_confirmed"->"邮箱尚未验证，请先打开注册验证邮件。"
  x is HttpFailure&&x.reason=="weak_password"->"这个密码未达到服务端要求，请换一个更长的密码。"
  x is HttpFailure&&x.reason=="same_password"->"邮箱已验证。这个密码就是原密码，请直接登录。"
  x is HttpFailure&&x.reason=="otp_expired"->"验证码或链接已失效，请重新申请邮件。"
  x is HttpFailure&&x.reason=="invalid_credentials"->"邮箱或密码不正确。"
  x is HttpFailure&&x.reason=="user_already_exists"->"请尝试登录，或通过邮件找回密码。"
  x is HttpFailure&&x.status in 500..599->"账号服务暂时不可用，请稍后再试。"
  x is HttpFailure->"请求未完成，请核对填写的信息后重试。"
  x is IOException->"网络连接失败，已保留输入，可以重试。"
  else->"无法完成账号操作，请重试。"
 }
}
internal data class AuthResult(val busy:Boolean=false,val tag:String="",val info:String="",val fail:Boolean=false,val email:String="",val sequence:Long=0)
