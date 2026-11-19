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
class RowGestureTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun longPressDragMovesTaskAndSwipeRevealsNamedActions(){val db=JournalDb.open(ui.activity);val j=Journal(db);for(t in listOf("drag-one","drag-two","drag-three"))j.create("todos",t);Journal.io.submit{}.get(6,TimeUnit.SECONDS);
 ui.waitUntil(8000){ui.onAllNodesWithText("drag-one").fetchSemanticsNodes().isNotEmpty()};val row=ui.onNodeWithText("drag-one");val distance=ui.onNodeWithText("drag-three").fetchSemanticsNode().boundsInRoot.center.y-row.fetchSemanticsNode().boundsInRoot.center.y;ui.onAllNodesWithContentDescription("排序标记")[0].performTouchInput{click()};assertFalse(Journal.io.submit<Boolean>{db.entries().rows("local","todos").first().done}.get());val second=ui.onNodeWithText("drag-two");val before=second.fetchSemanticsNode().boundsInRoot.center.y;
row.performTouchInput{down(center);advanceEventTime(850);moveBy(androidx.compose.ui.geometry.Offset(0f,distance),120)};
ui.waitUntil(5000){second.fetchSemanticsNode().boundsInRoot.center.y<before-8};assertEquals("drag-one",Journal.io.submit<String>{db.entries().rows("local","todos").first().title}.get());ui.onRoot().performTouchInput{up()};
 ui.waitUntil(4500){Journal.io.submit<Boolean>{db.entries().rows("local","todos").last().title=="drag-one"}.get()};row.performTouchInput{swipeLeft()};ui.onNodeWithContentDescription("删除").assertExists();ui.onNodeWithText("drag-two").performTouchInput{swipeRight()};ui.onNodeWithContentDescription("右滑完成").performClick();
 ui.waitUntil(4500){Journal.io.submit<Boolean>{db.entries().rows("local","todos").single{it.title=="drag-two"}.done}.get()}}
}