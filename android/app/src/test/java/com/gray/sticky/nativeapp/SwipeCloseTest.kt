package com.gray.sticky.nativeapp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],qualifiers="w390dp-h844dp")
class SwipeCloseTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun shortReverseSwipeAndTapBlankCloseAnOpenRow(){val j=Journal(JournalDb.open(ui.activity));j.create("todos","swipe-demo");Journal.io.submit{}.get();ui.waitUntil(6000){ui.onAllNodesWithText("swipe-demo").fetchSemanticsNodes().isNotEmpty()};val mark=ui.onNodeWithText("swipe-demo");val xy=mark.fetchSemanticsNode().boundsInRoot.center;val root=ui.onRoot();mark.performTouchInput{swipeLeft()};ui.onNodeWithContentDescription("删除").assertExists();root.performTouchInput{swipe(xy,xy+androidx.compose.ui.geometry.Offset(42.dp.toPx(),0f),220)};ui.onNodeWithContentDescription("删除").assertDoesNotExist();mark.performTouchInput{swipeLeft()};ui.onNodeWithContentDescription("删除").assertExists();root.performTouchInput{click(androidx.compose.ui.geometry.Offset(centerX,height-14f))};ui.onNodeWithContentDescription("删除").assertDoesNotExist()}
}