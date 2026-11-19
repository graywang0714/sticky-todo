package com.gray.sticky.nativeapp;
import androidx.room.*;import androidx.lifecycle.LiveData;import java.util.*;
@Dao public interface EntryDao{
 @Query("SELECT * FROM entries WHERE space=:space AND hidden=0 ORDER BY done,pinned DESC,CASE WHEN kind='notes' THEN -updated ELSE position END,created,id") LiveData<List<Entry>> watch(String space);
 @Query("SELECT * FROM entries WHERE space=:s AND kind=:k AND id=:id") Entry one(String s,String k,String id);
 @Query("SELECT * FROM entries WHERE space=:s AND kind=:kind ORDER BY done,pinned DESC,position,created,id") List<Entry> rows(String s,String kind);
 @Query("DELETE FROM entries WHERE substr(space,1,6)='trash:' AND updated<=:cutoff") void expire(long cutoff);
 @Insert(onConflict=OnConflictStrategy.REPLACE) void put(Entry e);
 @Query("DELETE FROM entries WHERE space=:s AND kind=:k AND id=:id") void remove(String s,String k,String id);
}