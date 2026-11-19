package com.gray.sticky.nativeapp

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.*
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.graphicsLayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity:ComponentActivity(){
 private lateinit var cloud:Cloud
 override fun onCreate(state:Bundle?){super.onCreate(state);window.statusBarColor=android.graphics.Color.rgb(67,57,34)
 cloud=Cloud.open(this);val v=mutableStateOf(CloudView());cloud.view.observe(this){v.value=it}
 val db=JournalDb.open(this)
 setContent { MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xff956200),onPrimary=Color.White,surface=Color(0xfffffcf4))){
 var panel by rememberSaveable{mutableStateOf(false)};var backup by rememberSaveable(v.value.space){mutableStateOf(false)}
 val space=if(backup)"backup:"+v.value.space else v.value.space
 key(space){val j=remember{Journal(db,space){cloud.kick()}};var entries by remember{mutableStateOf<List<Entry>>(emptyList())}
 DisposableEffect(j){val live=j.watch();val observer=androidx.lifecycle.Observer<List<Entry>>{entries=it.orEmpty()};live.observe(this@MainActivity,observer);onDispose{live.removeObserver(observer)}}
 Notebook(entries,j,if(backup)"冲突备份" else v.value.status,backup,{cloud.kick()},{if(backup)backup=false else panel=true})}
 if(panel)AccountPanel(v.value,cloud,{panel=false;backup=true},{panel=false})
 }}}
 override fun onStart(){super.onStart();cloud.activate(true)}
 override fun onStop(){cloud.activate(false);super.onStop()}
}
private data class DragPreview(val id:String,val order:List<String>,val from:Int,val to:Int,val distance:Float,val stride:Float,val landing:Float,val dropped:Boolean=false){
 fun shift(other:String):Float{val n=order.indexOf(other);return when{n<0->0f;other==id->if(dropped)landing else distance;n in to until from->stride;n>from&&n<=to-> -stride;else->0f}}
}
private val ink=Color(0xff332e25)
@Composable private fun Notebook(all:List<Entry>,j:Journal,status:String,backup:Boolean,refresh:()->Unit,account:()->Unit){
 var tab by rememberSaveable{mutableStateOf(if(backup)"notes" else "todos")};var quick by rememberSaveable{mutableStateOf("")}
 var editing by rememberSaveable{mutableStateOf<String?>(null)};var title by rememberSaveable{mutableStateOf("")};var body by rememberSaveable{mutableStateOf("")}
 var origTitle by rememberSaveable{mutableStateOf("")};var origBody by rememberSaveable{mutableStateOf("")};var editKind by rememberSaveable{mutableStateOf("todos")};var deleteKind by rememberSaveable{mutableStateOf("todos")};var seen by rememberSaveable{mutableStateOf("")};var deletion by rememberSaveable{mutableStateOf<String?>(null)};var archive by rememberSaveable{mutableStateOf(false)}
 val listState=rememberLazyListState();var openId by remember{mutableStateOf<String?>(null)};var showBin by rememberSaveable{mutableStateOf(false)};var recycled by remember{mutableStateOf<List<Entry>>(emptyList())}
 DisposableEffect(j){val l=j.bin();val ob=androidx.lifecycle.Observer<List<Entry>>{recycled=it.orEmpty()};l.observeForever(ob);onDispose{l.removeObserver(ob)}}
 LaunchedEffect(j){while(true){j.expire();kotlinx.coroutines.delay(60000)}}
 fun commitEditor(){val id=editing?:return;if(EntryPolicy.text(title)!=null||EntryPolicy.text(body)!=null){if(id=="NEW")j.create("notes",title,body) else j.save(editKind,id,title,body,origTitle,origBody)};editing=null}
 var order by remember(j){mutableStateOf<Entry?>(null)}
 DisposableEffect(j){val l=j.watchNoteOrder();val o=androidx.lifecycle.Observer<List<Entry>>{order=it.firstOrNull()};l.observeForever(o);onDispose{l.removeObserver(o)}}
 val tasks=all.filter{it.kind=="todos"};val notes=remember(all,order){j.orderedNotes(all.filter{it.kind=="notes"},order)}
 fun edit(e:Entry?){commitEditor();openId=null;editing=e?.id?:"NEW";title=e?.title?:"";body=e?.body?:"";origTitle=title;origBody=body;editKind=e?.kind?:"notes"}
 fun add(){if(EntryPolicy.text(quick)!=null){j.create("todos",quick);quick=""}}
 val amber=Color(0xfff59e0b);val soft=Color.White.copy(alpha=.54f)
 Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xfffaf0ae),Color(0xfff3e27f))))){
 val headingStyle=androidx.compose.ui.text.TextStyle(fontSize=21.sp,lineHeight=26.sp,fontWeight=FontWeight.Bold,color=ink);val measure=androidx.compose.ui.text.rememberTextMeasurer();val headingWidth=with(LocalDensity.current){measure.measure(androidx.compose.ui.text.AnnotatedString("📒 Sticky Notes"),style=headingStyle,softWrap=false,maxLines=1).size.width.toDp()};val heading:@Composable (Modifier)->Unit={m->Text("📒 Sticky Notes",m,maxLines=1,softWrap=false,style=headingStyle)}
 val actions:@Composable ()->Unit={Row(verticalAlignment=Alignment.CenterVertically){ IconButton(onClick=refresh,modifier=Modifier.semantics{contentDescription="手动同步"}){Text("↻",fontSize=22.sp)}
 IconButton(onClick=account,modifier=Modifier.semantics{contentDescription=if(backup)"返回" else "电脑同步"}){Canvas(Modifier.size(19.dp)){
 val c=Color(0xff716b62);val w=size.width;drawCircle(c,w*.16f,androidx.compose.ui.geometry.Offset(w*.5f,w*.26f),style=androidx.compose.ui.graphics.drawscope.Stroke(w*.09f));drawArc(c,180f,180f,false,androidx.compose.ui.geometry.Offset(w*.13f,w*.56f),androidx.compose.ui.geometry.Size(w*.74f,w*.62f),style=androidx.compose.ui.graphics.drawscope.Stroke(w*.09f))}}
 IconButton(onClick={commitEditor();openId=null;showBin=true},modifier=Modifier.semantics{contentDescription="回收站"}){Glyph("删除",Color(0xff716b62))}}}
 BoxWithConstraints(Modifier.fillMaxWidth().padding(start=16.dp,end=8.dp,top=8.dp)){val stacked=maxWidth<headingWidth+148.dp
 if(stacked){Column{heading(Modifier.fillMaxWidth());Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){actions()}}}else{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){heading(Modifier.weight(1f));actions()}}}
 Text(status,Modifier.padding(start=16.dp,end=16.dp,bottom=6.dp),fontSize=12.sp,lineHeight=15.sp,color=Color(0xff625c51))
 Row(Modifier.padding(start=16.dp,bottom=12.dp).background(soft,RoundedCornerShape(9.dp)).padding(3.dp)){
 for((key,label) in listOf("todos" to "待办","notes" to "随笔")){val selected=tab==key
 Text(label,Modifier.clip(RoundedCornerShape(7.dp)).background(if(selected)amber else Color.Transparent).clickable{commitEditor();openId=null;tab=key}.padding(horizontal=16.dp,vertical=7.dp),color=if(selected)Color.White else ink,fontSize=15.sp)}}
 if(tab=="todos"){
 Row(Modifier.fillMaxWidth().padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(quick,{quick=it},Modifier.weight(1f),shape=RoundedCornerShape(8.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color.White.copy(alpha=.74f),focusedContainerColor=Color.White,unfocusedBorderColor=Color(0xfff3d77a)),singleLine=true,placeholder={Text("随手记一条，回车保存…",fontSize=14.sp)},keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={add()})); Spacer(Modifier.width(8.dp));Button(onClick={add()},shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.buttonColors(containerColor=amber)){Text("记下",color=Color.White)}}
 }else{TextButton(onClick={edit(null)},modifier=Modifier.padding(horizontal=16.dp)){Text("＋ 写随笔")}}
 val items=if(tab=="todos")tasks.filter{!it.done}else notes
 val done=tasks.filter{it.done}.sortedByDescending{it.completed?:0}
 val editor:@Composable ()->Unit={Column(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(9.dp)).padding(10.dp)){OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text(if(editKind=="notes")"标题" else "待办内容")},singleLine=true);if(editKind=="notes")OutlinedTextField(body,{body=it},Modifier.fillMaxWidth(),label={Text("正文")},minLines=2,maxLines=7);Row{TextButton(onClick={commitEditor()},enabled=EntryPolicy.text(title)!=null||EntryPolicy.text(body)!=null){Text("保存")};TextButton(onClick={editing=null}){Text("取消")}}}}
 var drag by remember{mutableStateOf<DragPreview?>(null)}
 fun peers(id:String):List<String>{val entries=if(tab=="notes")notes else tasks;val e=entries.find{it.id==id}?:return emptyList();return entries.filter{!it.done&&it.pinned==e.pinned}.map{it.id}}
 val viewDrag=drag?.takeIf{it.order==peers(it.id)}
 LaunchedEffect(drag?.id,drag?.dropped,viewDrag==null){if(viewDrag==null){drag=null}else if(viewDrag.dropped){kotlinx.coroutines.delay(600);drag=null}}
 fun preview(e:Entry,dist:Float){val order=peers(e.id);val from=order.indexOf(e.id);val visible=listState.layoutInfo.visibleItemsInfo.filter{it.key in order};val src=visible.find{it.key==e.id}?:return;if(from<0)return
 val delta=dist.coerceIn((visible.first().offset-src.offset).toFloat(),(visible.last().offset+visible.last().size-src.offset-src.size).coerceAtLeast(0).toFloat());var dest=src
 if(delta<0){for(r in visible.filter{it.index<src.index}.asReversed()){if(src.offset+delta>r.offset+r.size/2f)break;dest=r}}else if(delta>0){for(r in visible.filter{it.index>src.index}){if(src.offset+src.size+delta<r.offset+r.size/2f)break;dest=r}}
 val to=order.indexOf(dest.key);if(to>=0)drag=DragPreview(e.id,order,from,to,delta,(src.size+listState.layoutInfo.mainAxisItemSpacing).toFloat(),(if(to>from)dest.offset+dest.size-src.size-src.offset else dest.offset-src.offset).toFloat())}
 val display:@Composable (Entry)->Unit={e->if(editing==e.id&&editKind==e.kind)editor()else EntryRow(e,viewDrag,openId==e.id,{openId=it},{edit(e)},{j.check(e.id)},{j.pin(e.kind,e.id)},{deleteKind=e.kind;seen=Wire.canon(e);deletion=e.id},{y->preview(e,y)},{save->val v=drag;if(save&&v!=null&&v.id==e.id&&v.order==peers(e.id)&&v.from!=v.to){drag=v.copy(dropped=true);if(e.kind=="notes")j.moveNoteOnto(e.id,v.order[v.to]) else j.moveOnto(e.id,v.order[v.to])}else drag=null})}
 LazyColumn(Modifier.fillMaxSize().pointerInput(Unit){awaitEachGesture{val down=awaitFirstDown(false,PointerEventPass.Final);var tap=!down.isConsumed;do{val ev=awaitPointerEvent(PointerEventPass.Final);val ch=ev.changes.first();if(ch.isConsumed||(ch.position-down.position).getDistance()>viewConfiguration.touchSlop)tap=false}while(ev.changes.any{it.pressed});if(tap)openId=null}},state=listState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
 if(editing=="NEW"||editing!=null&&all.none{it.id==editing&&it.kind==editKind})item{editor()};if(items.isEmpty()&&editing==null)item{Text(if(status.contains("暂未同步")||status.contains("正在同步")||status=="待同步")status else if(tab=="todos")"暂无待办，享受当下 ✨" else "暂无随笔",Modifier.padding(vertical=18.dp),color=Color.DarkGray)}
 items(items,key={it.id}){display(it)};if(tab=="todos"&&done.isNotEmpty()){item{TextButton(onClick={archive=!archive}){Text((if(archive)"⌄ " else "› ")+"回顾 · 已完成 "+done.size+" 项")}};if(archive)done.groupBy{SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(Date(it.completed?:it.updated))}.forEach{(day,rows)->item{Text(day,color=Color.DarkGray,fontSize=13.sp)};items(rows,key={it.id}){display(it)}}}}}
 if(showBin){AlertDialog(onDismissRequest={showBin=false},title={Text("回收站")},text={Column{Text("仅保留此设备删除的记录，30 天后自动清理。");LazyColumn(Modifier.heightIn(max=360.dp)){if(recycled.isEmpty())item{Text("回收站为空")};items(recycled,key={it.id}){b->Row(verticalAlignment=Alignment.CenterVertically){Text(b.title,Modifier.weight(1f));TextButton(onClick={j.restore(b.kind,b.id)}){Text("恢复")}}}}}},confirmButton={TextButton(onClick={showBin=false}){Text("关闭")}})}
 if(deletion!=null){AlertDialog(onDismissRequest={deletion=null},title={Text("删除这条记录？")},text={Text("将移入本机回收站，30 天内可恢复。")},confirmButton={TextButton(onClick={j.remove(deleteKind,deletion!!,seen);deletion=null}){Text("删除")}},dismissButton={TextButton(onClick={deletion=null}){Text("保留")}})}
}
@Composable private fun EntryRow(e:Entry,preview:DragPreview?,opened:Boolean,onOpen:(String?)->Unit,edit:()->Unit,check:()->Unit,pin:()->Unit,del:()->Unit,move:(Float)->Unit,finish:(Boolean)->Unit){
 var menu by remember{mutableStateOf(false)};var offset by remember(e.id){mutableFloatStateOf(0f)}
 LaunchedEffect(opened){if(!opened)offset=0f};val currentOpen by rememberUpdatedState(onOpen)
 val reach=with(LocalDensity.current){114.dp.toPx()};val isTodo=e.kind=="todos"&&!e.done
 var dy by remember(e.id){mutableFloatStateOf(0f)};var lifting by remember(e.id){mutableStateOf(false)};val currentMove by rememberUpdatedState(move);val end by rememberUpdatedState(finish);val shift=key(preview?.id){val x=preview?.shift(e.id)?:0f;if(preview?.id==e.id||preview==null)x else animateFloatAsState(x,spring(dampingRatio=.92f,stiffness=440f),label="让位").value};val eased by animateFloatAsState(offset,spring(dampingRatio=.95f,stiffness=320f),label="row swipe")
 Box(Modifier.fillMaxWidth().zIndex(if(preview?.id==e.id)2f else 0f) .then(if(preview!=null)Modifier else Modifier.clip(RoundedCornerShape(9.dp))).then(if(isTodo||e.kind=="notes")Modifier.pointerInput(e.id){detectDragGesturesAfterLongPress(onDragStart={lifting=true;dy=0f;offset=0f;currentOpen(null);currentMove(0f)},onDragCancel={end(false);dy=0f;lifting=false},onDragEnd={end(true);dy=0f;lifting=false}){ch,delta->ch.consume();dy+=delta.y;currentMove(dy)}}else Modifier).then(if(isTodo)Modifier.pointerInput(e.id,reach){var start=0f;var travel=0f;detectHorizontalDragGestures(onDragStart={start=offset;travel=0f;currentOpen(e.id)},onDragEnd={offset=when{start!=0f&&travel*start<0&&kotlin.math.abs(travel)>10.dp.toPx()->0f;offset>reach*.22f->reach*.6f;offset< -reach*.22f-> -reach;else->0f};if(offset==0f)currentOpen(null)},onDragCancel={offset=0f;currentOpen(null)}){p,d->p.consume();travel+=d;offset=(start+travel*(if(start!=0f&&travel*start<0)1.15f else .76f)).coerceIn(-reach,reach*.6f)}}else Modifier)){
 if(isTodo&&eased!=0f){Row(Modifier.matchParentSize().padding(horizontal=4.dp),horizontalArrangement=if(eased>0)Arrangement.Start else Arrangement.End,verticalAlignment=Alignment.CenterVertically){
 if(eased>0)ActionGlyph("完成",Color(0xff248859)){offset=0f;check()}else{ActionGlyph(if(e.pinned)"取消置顶" else "置顶",if(e.pinned)Color(0xff625e72) else Color(0xffac7100)){offset=0f;pin()};ActionGlyph("删除",Color(0xffbd4039)){offset=0f;del()}}}}
 Surface(modifier=Modifier.fillMaxWidth().graphicsLayer{scaleX=if(lifting).97f else 1f;scaleY=scaleX;alpha=if(lifting).8f else 1f;translationY=shift}.offset{IntOffset(eased.roundToInt(),0)},shape=RoundedCornerShape(9.dp),color=if(e.done)Color.White.copy(alpha=.48f) else Color.White){
 Row(Modifier.fillMaxWidth().padding(horizontal=6.dp),verticalAlignment=Alignment.CenterVertically){
 if(e.kind=="todos"){Box(Modifier.size(34.dp).then(if(e.done)Modifier.clickable(onClick=check) else Modifier).semantics{contentDescription=if(e.done)"回退到待办" else "排序标记"},contentAlignment=Alignment.Center){if(e.done)Text("↶",color=Color.Gray)else if(e.pinned)Glyph("置顶",Color(0xffb47b10)) else Box(Modifier.size(6.dp).background(Color(0xffaaa8a3),RoundedCornerShape(50)))}}
 Column(Modifier.weight(1f).clickable{if(offset!=0f)offset=0f else edit()}.padding(horizontal=4.dp,vertical=12.dp)){
 Text(e.title,color=if(e.done)Color.Gray else ink,fontSize=16.sp,fontWeight=if(e.pinned)FontWeight.Bold else FontWeight.Normal,textDecoration=if(e.done)TextDecoration.LineThrough else null)
 if(e.kind=="notes"&&e.body.isNotBlank())Text(e.body,maxLines=2,color=Color.DarkGray,fontSize=14.sp)}
 Box{IconButton(onClick={menu=true},modifier=Modifier.size(34.dp).semantics{contentDescription="更多操作"}){Text("⋯",color=Color.Gray)};DropdownMenu(menu,{menu=false}){listOf("编辑" to edit,(if(e.pinned)"取消置顶" else "置顶") to pin).forEach{(s,fn)->DropdownMenuItem(text={Text(s)},onClick={menu=false;fn()})};DropdownMenuItem(text={Text("删除")},onClick={menu=false;del()})}}
 }}}
}
@Composable private fun ActionGlyph(label:String,c:Color,action:()->Unit){IconButton(onClick=action,modifier=Modifier.size(44.dp).semantics{contentDescription=if(label=="完成")"右滑完成" else label}){Box(Modifier.size(30.dp).background(c,RoundedCornerShape(8.dp)),contentAlignment=Alignment.Center){Glyph(label,Color.White)}}}
@Composable private fun Glyph(label:String,c:Color){Canvas(Modifier.size(17.dp)){val u=size.minDimension/24;val points=when(label){"完成"->listOf(3 to 12,9 to 18,21 to 5);"删除"->listOf(4 to 6,20 to 6,17 to 6,16 to 21,8 to 21,7 to 6);else->listOf(7 to 3,17 to 3,15 to 6,15 to 10,19 to 14,19 to 16,5 to 16,5 to 14,9 to 10,9 to 6,7 to 3)};for((a,b) in points.zipWithNext())drawLine(c,androidx.compose.ui.geometry.Offset(a.first*u,a.second*u),androidx.compose.ui.geometry.Offset(b.first*u,b.second*u),2*u);if(label.contains("置顶"))drawLine(c,androidx.compose.ui.geometry.Offset(12*u,16*u),androidx.compose.ui.geometry.Offset(12*u,22*u),2*u);if(label=="取消置顶")drawLine(c,androidx.compose.ui.geometry.Offset(2*u,2*u),androidx.compose.ui.geometry.Offset(22*u,22*u),2.5f*u);if(label=="删除")drawLine(c,androidx.compose.ui.geometry.Offset(9*u,3*u),androidx.compose.ui.geometry.Offset(15*u,3*u),2*u)}}
