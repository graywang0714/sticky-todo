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
class TrashUiTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun editInRowRestoreAndExpire(){val ctx=ui.activity;val db=JournalDb.open(ctx);val j=Journal(db);fun drain(){Journal.io.submit{}.get(8,TimeUnit.SECONDS)};j.create("notes","trash-demo","body");drain();ui.onNodeWithText("随笔").performClick();ui.waitUntil(6000){ui.onAllNodesWithText("trash-demo").fetchSemanticsNodes().isNotEmpty()};ui.onNodeWithText("trash-demo").performClick();ui.onAllNodes(isDialog()).assertCountEquals(0);ui.onNodeWithText("正文").performTextReplacement("edited");ui.onNodeWithText("保存").performClick();drain();val id=Journal.io.submit<String>{db.entries().rows("local","notes").single().id}.get();val snapshot=Journal.io.submit<String>{Wire.canon(db.entries().one("local","notes",id))}.get();j.remove("notes",id,snapshot);drain();ui.onNodeWithContentDescription("回收站").performClick();ui.onNodeWithText("恢复").performClick();drain();ui.onNodeWithText("关闭").performClick();ui.onNodeWithText("edited").assertExists();Journal.io.submit{val e=Entry().apply{kind="notes";space="trash:local";this.id="expired";updated=System.currentTimeMillis()-31L*86400000};db.entries().put(e)}.get();j.expire();drain();assertTrue(Journal.io.submit<Boolean>{db.entries().rows("trash:local","notes").isEmpty()}.get())}
}