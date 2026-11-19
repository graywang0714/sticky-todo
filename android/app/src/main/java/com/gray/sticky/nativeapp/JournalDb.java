package com.gray.sticky.nativeapp;
import androidx.room.*;import android.content.Context;
@Database(entities={Entry.class},version=1,exportSchema=false)
public abstract class JournalDb extends RoomDatabase {
 public abstract EntryDao entries();
 private static volatile JournalDb instance;
 public static synchronized JournalDb open(Context c){if(instance==null)instance=Room.databaseBuilder(c.getApplicationContext(),JournalDb.class,"sticky-native.db").build();return instance;}
}