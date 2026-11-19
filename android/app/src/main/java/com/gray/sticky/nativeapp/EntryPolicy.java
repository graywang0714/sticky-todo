package com.gray.sticky.nativeapp;
public final class EntryPolicy {
 private EntryPolicy() {}
 public static String text(String s){if(s==null)return null;int a=0,b=s.length();while(a<b&&(Character.isWhitespace(s.charAt(a))||Character.isSpaceChar(s.charAt(a))))a++;while(b>a&&(Character.isWhitespace(s.charAt(b-1))||Character.isSpaceChar(s.charAt(b-1))))b--;return a==b?null:s.substring(a,b);}
 public static Long completion(boolean done,long now){return done?Long.valueOf(now):null;}
}