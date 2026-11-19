package com.gray.sticky.nativeapp;
import org.junit.Test;import static org.junit.Assert.*;
public class DraftTest{
 @Test public void phoneOnlyEditedTitleKeepsDesktopBody(){assertEquals("手机标题",Draft.merge("原题","手机标题","原题"));assertEquals("电脑正文",Draft.merge("旧正文","旧正文","电脑正文"));}
 @Test public void twoDifferentChangesToSameFieldConflict(){assertNull(Draft.merge("初稿","手机稿","电脑稿"));}
 @Test public void equalEditsAreNotConflict(){assertEquals("新",Draft.merge("旧","新","新"));}
}