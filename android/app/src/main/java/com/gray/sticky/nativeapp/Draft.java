package com.gray.sticky.nativeapp;
public final class Draft {
 public static String merge(String initial,String mine,String latest){if(mine.equals(initial))return latest;if(latest.equals(initial)||latest.equals(mine))return mine;return null;}
}