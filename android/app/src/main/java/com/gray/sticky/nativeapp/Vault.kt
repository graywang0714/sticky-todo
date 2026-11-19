package com.gray.sticky.nativeapp

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

internal class Vault(ctx: Context) {
 private val prefs=ctx.getSharedPreferences("cloud_session",Context.MODE_PRIVATE)
 private val alias="sticky-native.session.v1"
 private fun key():SecretKey { val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
  (store.getKey(alias,null) as? SecretKey)?.let{return it}
  return KeyGenerator.getInstance("AES","AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build())}.generateKey()
 }
 private fun enc(b:ByteArray)=Base64.encodeToString(b,Base64.NO_WRAP)
 fun load():JSONObject? { val saved=prefs.getString("session",null)?:return null
  val (iv,ct)=saved.split(':',limit=2);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)))
  return JSONObject(String(c.doFinal(Base64.decode(ct,Base64.NO_WRAP)),Charsets.UTF_8))
 }
 fun save(x:JSONObject?){if(x==null){check(prefs.edit().remove("session").commit());return}
  val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());val ct=c.doFinal(x.toString().toByteArray(Charsets.UTF_8))
  check(prefs.edit().putString("session",enc(c.iv)+":"+enc(ct)).commit())
 }
}