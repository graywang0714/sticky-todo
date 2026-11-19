package com.gray.sticky.nativeapp;
import java.util.Objects;
public final class Reconcile {
 public static String action(String base,String local,String remote){
  if(Objects.equals(local,remote))return "ACK";
  if(Objects.equals(base,local))return "ADOPT";
  return Objects.equals(base,remote)?"SEND":"CONFLICT";
 }
}