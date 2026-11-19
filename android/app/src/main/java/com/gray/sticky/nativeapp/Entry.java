package com.gray.sticky.nativeapp;
import androidx.room.*;import androidx.annotation.NonNull;
@Entity(tableName="entries",primaryKeys={"space","kind","id"})
public class Entry {
 @NonNull public String id="",space="local",kind="todos",title="",body="";
 public boolean done,pinned,dirty,hidden; public String shadow;
 public long created,updated;
 public double position;
 public Long completed;
}