package com.gray.sticky.nativeapp
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

class AuthFlowTest {
 @Test fun recoveryLinkMustBelongToOurProject(){val u="https://sample.supabase.co".toHttpUrl()
 assertThrows(IllegalArgumentException::class.java){AuthFlow.proof(u,"https://different.supabase.co/auth/v1/verify?type=recovery&token=abcdefgh12345678","a@b.co")}
 assertThrows(IllegalArgumentException::class.java){AuthFlow.proof(u,"https://sample.supabase.co/auth/v1/verify?type=signup&token=abcdefgh12345678","a@b.co")}
 assertEquals("abcdefgh12345678",AuthFlow.proof(u,"https://sample.supabase.co/auth/v1/verify?type=recovery&token=abcdefgh12345678","a@b.co").getString("token_hash"))
 assertEquals("123456",AuthFlow.proof(u,"123456","a@b.co").getString("token"))
 }
 @Test fun registrationAndResetUseDedicatedEndpoints(){MockWebServer().use{s->s.start();val a=CloudApi(s.url("/"),"public");s.enqueue(MockResponse().setBody("{\"id\":\"sample\"}"));val res=a.auth("signup",JSONObject().put("email","a@b.co").put("password","exampletest"));assertFalse(res.has("access_token"));assertEquals("/auth/v1/signup",s.takeRequest().path)
 s.enqueue(MockResponse().setBody("{}"));a.auth("recover",JSONObject().put("email","a@b.co"));val q=s.takeRequest();assertEquals("/auth/v1/recover",q.path);assertFalse(JSONObject(q.body.readUtf8()).has("password"))
 s.enqueue(MockResponse().setBody("{}"));a.setPassword("verified-session","newpassword");val upd=s.takeRequest();assertEquals("PUT",upd.method);assertEquals("Bearer verified-session",upd.getHeader("Authorization"));assertEquals("/auth/v1/user",upd.path)}}
 @Test fun temporaryServiceFailureDoesNotBecomeWrongPassword(){assertNotEquals(AuthFlow.message(HttpFailure(503)),AuthFlow.message(HttpFailure(400,"invalid_credentials")));assertTrue(AuthFlow.message(HttpFailure(429)).contains("稍后"))}
 @Test fun gmailWrap(){val base="https://sample.supabase.co".toHttpUrl();val token="synthetic_fixture_123456";val wrapped="google.com/url?q=https://sample.supabase.co/auth/v1/verify?token%3D"+token+"%26type%3Drecovery%26redirect_to%3Dhttp://localhost:3000&source=gmail";assertEquals(token,AuthFlow.proof(base,wrapped,"a@b.co").getString("token_hash"));assertThrows(IllegalArgumentException::class.java){AuthFlow.proof(base,wrapped.replace("sample.supabase.co","other.invalid"),"a@b.co")};assertThrows(IllegalArgumentException::class.java){AuthFlow.proof(base,wrapped,"a@b.co","email")}}
}