package com.gray.sticky.nativeapp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],qualifiers="w390dp-h844dp")
class AccountIsolationUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun pendingEditsSurviveAtoBtoAWithoutCrossingSpaces(){
  val db=JournalDb.open(ui.activity);val cloud=Cloud.open(ui.activity)
  fun sql(f:()->Unit){Journal.io.submit{f()}.get(10,TimeUnit.SECONDS)}
  fun select(space:String){ui.runOnIdle{cloud.view.value=CloudView(space=space)}}
  fun visible(label:String){ui.waitUntil(8000){ui.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText(label).assertIsDisplayed()}
  fun absent(label:String){ui.onNodeWithText(label).assertDoesNotExist()}
  val prior=cloud.view.value?:CloudView();try {
  sql{for(s in listOf("local","A","B")) for(k in listOf("todos","notes")) {
   db.entries().put(Entry().apply{space=s;kind=k;id="same-id";title=s+"-"+k;body="";created=1;dirty=s!="local"})
  }}
  visible("local-todos");select("A");visible("A-todos");absent("B-todos");absent("local-todos")
  Journal(db,"A").save("todos","same-id","A-pending","","A-todos","");sql{};visible("A-pending")
  select("local");visible("local-todos");absent("A-pending")
  select("B");visible("B-todos");absent("A-pending");ui.onNodeWithText("随笔").performClick();visible("B-notes");absent("A-notes")
  Journal(db,"B").check("same-id");sql{}
  select("A");visible("A-pending");absent("B-notes");ui.onNodeWithText("随笔").performClick();visible("A-notes");absent("B-notes")
  sql{val dao=db.entries();assertTrue(dao.one("A","todos","same-id").dirty);assertFalse(dao.one("A","todos","same-id").done);assertTrue(dao.one("B","todos","same-id").done);assertEquals("local-todos",dao.one("local","todos","same-id").title)}
  } finally { ui.runOnIdle{cloud.view.value=prior};sql{for(s in listOf("local","A","B"))for(k in listOf("todos","notes"))db.entries().remove(s,k,"same-id")} }
 }

}
