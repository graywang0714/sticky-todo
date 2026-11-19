package com.gray.sticky.nativeapp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],qualifiers="w390dp-h844dp")
class LoginUiTest{
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun canValidateFormsAndReturnToOfflineWithoutNetwork(){
  ui.onNodeWithContentDescription("电脑同步").performClick()
  ui.onNodeWithText("登录 Sticky Notes").assertIsDisplayed()
  ui.onNodeWithText("登录并同步").performClick()
  ui.onNodeWithText("请填写有效的邮箱地址。").assertIsDisplayed()
  ui.onNodeWithText("注册账号").performClick()
  ui.onNodeWithText("发送验证邮件").assertIsDisplayed();ui.onNodeWithText("再次输入密码").assertDoesNotExist()
  ui.onNodeWithText("返回登录").performClick()
  ui.onNodeWithText("忘记密码？").performClick()
  ui.onNodeWithText("发送恢复邮件").assertIsDisplayed()
  ui.onNodeWithText("返回笔记本").performClick()
  ui.onNodeWithContentDescription("电脑同步").assertIsDisplayed()
 }
}