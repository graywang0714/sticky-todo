package com.gray.sticky.nativeapp;
import static org.junit.Assert.*;import org.junit.Test;
public class ReconcileTest {
 @Test public void remoteEditDuringOfflineEditIsPreserved(){assertEquals("CONFLICT",Reconcile.action("old","local","remote"));}
 @Test public void retriedSuccessfulWriteIsAcknowledged(){assertEquals("ACK",Reconcile.action("old","mine","mine"));}
 @Test public void offlineDeleteAgainstModifiedCloudIsNotSent(){assertEquals("CONFLICT",Reconcile.action("old",null,"remote"));}
 @Test public void newLocalItemMayBeSent(){assertEquals("SEND",Reconcile.action(null,"new",null));}
 @Test public void remoteDeletedAndUnchangedLocalIsAdopted(){assertEquals("ADOPT",Reconcile.action("old","old",null));}
}