package com.gray.sticky.nativeapp;
import org.junit.Test; import static org.junit.Assert.*;
public class EntryPolicyTest {
 @Test public void whitespaceOnlyIsNotAnEntry(){assertNull(EntryPolicy.text("　 \t\n"));}
 @Test public void preserveInternalSpacing(){assertEquals("买 牛奶",EntryPolicy.text("  买 牛奶  "));}
 @Test public void completingAndUndoingSetArchiveTime(){assertEquals(Long.valueOf(123L),EntryPolicy.completion(true,123L));assertNull(EntryPolicy.completion(false,124L));}
}