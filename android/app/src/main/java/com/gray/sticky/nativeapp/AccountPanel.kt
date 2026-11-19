package com.gray.sticky.nativeapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer

@Composable internal fun AccountPanel(v:CloudView,c:Cloud,backup:()->Unit,onClose:()->Unit){
 val owner=androidx.compose.ui.platform.LocalLifecycleOwner.current
 var result by remember{mutableStateOf(c.authResult.value?:AuthResult())};val opened=remember{(c.authResult.value?:AuthResult()).sequence}
 DisposableEffect(c,owner){val o=Observer<AuthResult>{result=it};c.authResult.observe(owner,o);onDispose{c.authResult.removeObserver(o)}}
 var mode by rememberSaveable{mutableStateOf(if(v.space!="local"&&!v.needLogin)"account" else "login")}
 var mail by rememberSaveable{mutableStateOf(v.email)};var pw by remember{mutableStateOf("")};var repeat by remember{mutableStateOf("")};var proof by remember{mutableStateOf("")}
 var visible by remember{mutableStateOf(false)};var err by rememberSaveable{mutableStateOf("")};var hint by rememberSaveable{mutableStateOf("")};var isError by remember{mutableStateOf(false)}
 var signout by remember{mutableStateOf(false)};val busy=result.busy
 fun switch(m:String){mode=m;err="";hint="";isError=false;pw="";repeat="";proof="";visible=false}
 LaunchedEffect(v.space,v.needLogin){if(mode=="account"&&(v.needLogin||v.space=="local")){switch("login");mail=v.email;if(v.needLogin)hint="登录已过期，重新登录后会继续同步。"}}
 LaunchedEffect(result.sequence,result.busy){if(result.sequence>opened&&!result.busy){err="";hint=result.info;isError=result.fail
  if(!result.fail)when(result.tag){"session"->{pw="";onClose()};"confirmation"->{switch("confirm");hint=result.info};"sent"->{switch("reset");hint=result.info};"changed"->{switch("login");hint=result.info};"logout"->onClose()}}}
 fun submit(){if(busy)return
  err=when{!AuthFlow.email(mail)->"请填写有效的邮箱地址。";mode in listOf("login","confirm","reset")&&pw.isEmpty()->"请输入密码。";mode in listOf("confirm","reset")&&pw.length<8->"新密码至少需要 8 个字符。";mode in listOf("confirm","reset")&&pw!=repeat->"两次输入的密码不一致。";mode in listOf("confirm","reset")&&proof.isBlank()->"请填写邮件里的验证码或恢复链接。";else->""}
  if(err.isNotEmpty()){hint="";return};hint="";c.accountAction(mode,mail.trim(),pw,proof)
 }
 Dialog(onDismissRequest={if(!busy)onClose()},properties=DialogProperties(usePlatformDefaultWidth=false)){
 Surface(Modifier.fillMaxSize(),color=Color(0xfffcf9ef)){
 Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Sticky Notes  /  账号",color=Color(0xff63562c),fontSize=15.sp,fontWeight=FontWeight.Medium);TextButton(enabled=!busy,onClick=onClose){Text("返回笔记本")}}
 val heading=when(mode){"confirm"->"验证邮箱并设置密码";"register"->"注册账号";"recover"->"找回密码";"reset"->"设置新密码";"account"->"我的账号";else->"登录 Sticky Notes"}
 Text(heading,fontSize=32.sp,lineHeight=40.sp,fontWeight=FontWeight.Bold,color=Color(0xff332e25))
 Text(when(mode){"confirm"->"只在此处输入你自己邮箱收到的验证码或未打开的验证链接。";"register"->"先验证邮箱归属，再设置密码。已有账号请直接登录。";"recover"->"输入账号邮箱。收到邮件后，回到这里输入验证码，或长按复制邮件按钮的链接；不用在浏览器里打开。";"reset"->"填入邮件里的验证码，或长按复制未打开的恢复链接（支持 Gmail 包装链接）。";"account"->v.email;else->"用电脑版的账号继续。你在本机记下的内容会单独保留。"},color=Color(0xff625b4e),lineHeight=22.sp)
 if(mode=="account"){HorizontalDivider();Text("同步状态："+v.status);Button(onClick={c.kick();onClose()},modifier=Modifier.fillMaxWidth()){Text("立即同步")};OutlinedButton(onClick=backup,modifier=Modifier.fillMaxWidth()){Text("查看冲突备份")};Text("冲突备份：两端修改同一条且不能合并时，保留手机版本供你核对。",fontSize=13.sp);TextButton(onClick={signout=true}){Text("退出账号，回到本机笔记本")}}
 else{
 OutlinedTextField(mail,{mail=it;err=""},Modifier.fillMaxWidth(),label={Text("邮箱地址")},singleLine=true,enabled=!busy&&mode !in listOf("confirm","reset"),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next))
 if(mode in listOf("confirm","reset"))OutlinedTextField(proof,{proof=it;err=""},Modifier.fillMaxWidth(),label={Text("验证码或恢复链接")},singleLine=true,enabled=!busy,visualTransformation=PasswordVisualTransformation())
 if(mode !in listOf("recover","register"))OutlinedTextField(pw,{pw=it;err=""},Modifier.fillMaxWidth(),label={Text(if(mode=="reset")"新密码" else "密码")},singleLine=true,enabled=!busy,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),trailingIcon={TextButton(onClick={visible=!visible}){Text(if(visible)"隐藏" else "显示")}},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=if(mode=="login")ImeAction.Done else ImeAction.Next),keyboardActions=KeyboardActions(onDone={submit()}))
 if(mode in listOf("confirm","reset"))OutlinedTextField(repeat,{repeat=it;err=""},Modifier.fillMaxWidth(),label={Text("再次输入密码")},singleLine=true,enabled=!busy,visualTransformation=if(visible)VisualTransformation.None else PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={submit()}))
 if(err.isNotEmpty()||hint.isNotEmpty()){val failed=err.isNotEmpty()||isError;Surface(shape=RoundedCornerShape(10.dp),color=if(failed)Color(0xffffe9dd)else Color(0xffedf1da)){Text(err.ifEmpty{hint},Modifier.fillMaxWidth().semantics{liveRegion=LiveRegionMode.Polite}.padding(12.dp),color=if(failed)Color(0xff802f18)else Color(0xff414a27))}}
 Button(onClick={submit()},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp)){if(busy){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp))};Text(if(busy)"正在处理…" else when(mode){"confirm"->"验证并完成注册";"register"->"发送验证邮件";"recover"->"发送恢复邮件";"reset"->"保存新密码";else->"登录并同步"})}
 if(mode=="login") {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(enabled=!busy,onClick={switch("register")}){Text("注册账号")};TextButton(enabled=!busy,onClick={switch("recover")}){Text("忘记密码？")}}}else{TextButton(enabled=!busy,onClick={switch("login")}){Text("返回登录")};if(mode=="reset")TextButton(enabled=!busy,onClick={switch("recover")}){Text("重新申请邮件")}}
 if(v.space!="local"&&v.needLogin)TextButton(enabled=!busy,onClick={signout=true}){Text("退出账号，使用本机笔记本")}
 }
 }}}
 if(signout)AlertDialog(onDismissRequest={signout=false},title={Text("退出这个账号？")},text={Text("本机缓存和待同步的编辑会保留，下次登录同一账号可以继续。")},confirmButton={TextButton(onClick={signout=false;c.logout();onClose()}){Text("退出账号")}},dismissButton={TextButton(onClick={signout=false}){Text("取消")}})
}