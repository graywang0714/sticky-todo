package com.gray.sticky.nativeapp

import androidx.room.Room
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class) @Config(sdk=[28],manifest=Config.NONE)
class LocalPersistenceTest{
 private fun drain(){Journal.io.submit{}.get(10,TimeUnit.SECONDS)}
 @Test fun reopensCompletedTodoWithoutMixingSpaces(){val ctx=RuntimeEnvironment.getApplication();val name="persistence-test.db"
  fun open()=Room.databaseBuilder(ctx,JournalDb::class.java,name).allowMainThreadQueries().build()
  var db=open()
  try {var j=Journal(db);j.create("todos","下楼取件");drain();val id=db.entries().rows("local","todos").single().id
   j.check(id);drain();db.close();db=open();val reopened=db.entries().one("local","todos",id);assertEquals("下楼取件",reopened.title);assertTrue(reopened.done);assertNotNull(reopened.completed);assertNull(db.entries().one("other","todos",id))
   j=Journal(db);j.remove("todos",id,Wire.canon(reopened));drain();assertNull(db.entries().one("local","todos",id))
  }finally{db.close();ctx.deleteDatabase(name)}}
 @Test fun staleEditorPreservesRemoteBodyAndConflictsAreRecoverable(){val ctx=RuntimeEnvironment.getApplication();val db=Room.inMemoryDatabaseBuilder(ctx,JournalDb::class.java).allowMainThreadQueries().build()
  try{val dao=db.entries();val j=Journal(db,"user");j.create("notes","原题","原文");drain();val e=dao.rows("user","notes").single();e.body="电脑修改";dao.put(e)
   j.save("notes",e.id,"手机标题","原文","原题","原文");drain();val merged=dao.one("user","notes",e.id);assertEquals("手机标题",merged.title);assertEquals("电脑修改",merged.body)
   j.save("notes",e.id,"原题","手机不同修改","原题","原文");drain();assertEquals("电脑修改",dao.one("user","notes",e.id).body);val backup=dao.rows("backup:user","notes").single();assertTrue(backup.body.contains("手机不同修改"))
   Journal(db,"backup:user").remove("notes",backup.id,Wire.canon(backup));drain();assertTrue(dao.rows("backup:user","notes").isEmpty())
  }finally{db.close()}}
}