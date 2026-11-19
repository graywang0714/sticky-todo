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
class NoteDragTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun notesMakeRoomBeforeReleaseAndRetainOrderAcrossTabs(){val db=JournalDb.open(ui.activity);val j=Journal(db);for(t in listOf("note-a","note-b","note-c"))j.create("notes",t,t);Journal.io.submit{}.get();ui.onNodeWithText("随笔").performClick();val first=ui.onAllNodesWithText("note-c").onFirst();ui.waitUntil(6000){ui.onAllNodesWithText("note-a").fetchSemanticsNodes().isNotEmpty()};val next=ui.onAllNodesWithText("note-b").onFirst();val startY=next.fetchSemanticsNode().boundsInRoot.center.y;val distance=ui.onAllNodesWithText("note-a").onFirst().fetchSemanticsNode().boundsInRoot.center.y-first.fetchSemanticsNode().boundsInRoot.center.y;first.performTouchInput{down(center);advanceEventTime(850);moveBy(androidx.compose.ui.geometry.Offset(0f,distance),120)};ui.waitUntil(4000){next.fetchSemanticsNode().boundsInRoot.center.y<startY-8};ui.onRoot().performTouchInput{up()};ui.waitUntil(4000){Journal.io.submit<Boolean>{val r=db.entries().one("order:local","order","notes");r!=null&&j.orderedNotes(db.entries().rows("local","notes"),r).last().title=="note-c"}.get()};ui.onNodeWithText("待办").performClick();ui.onNodeWithText("随笔").performClick();ui.waitUntil(4000){first.fetchSemanticsNode().boundsInRoot.center.y>next.fetchSemanticsNode().boundsInRoot.center.y};val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>();ui.onNodeWithText("📒 Sticky Notes").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){it(layouts)};assertEquals(1,layouts.single().lineCount);assertFalse("heading measured "+layouts.single().size+" constraints "+layouts.single().layoutInput.constraints+" lineEnd "+layouts.single().getLineEnd(0),layouts.single().didOverflowWidth);ui.onNodeWithContentDescription("回收站").performClick();ui.onNodeWithText("回收站").assertExists()}
}