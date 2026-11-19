package com.gray.sticky.nativeapp
import org.junit.Test
import org.junit.Assert.*
import okhttp3.mockwebserver.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

class CloudApiTest {
 @Test fun desktopShapeAndAccountFence(){MockWebServer().use{s->
 s.start();val a=CloudApi(s.url("/"),"public-test");val o=JSONObject().put("id","test").put("user_id","one").put("text","买牛奶").put("done",false).put("pinned",true).put("position",0).put("created_at",100L).put("completed_at",JSONObject.NULL)
 s.enqueue(MockResponse().setBody("["+o+"]"));val row=a.list("one","token","todos").getValue("test")
 assertEquals("买牛奶",row.title);assertTrue(row.pinned);val req=s.takeRequest();assertEquals("eq.one",req.requestUrl!!.queryParameter("user_id"))
 row.shadow=Wire.canon(row);row.done=true;row.completed=123;s.enqueue(MockResponse().setBody("[{}]"))
 assertTrue(a.mutate("one","token",row));val patch=s.takeRequest();assertEquals("PATCH",patch.method);assertEquals("eq.false",patch.requestUrl!!.queryParameter("done"));assertEquals("is.null",patch.requestUrl!!.queryParameter("completed_at"));assertEquals("eq.one",patch.requestUrl!!.queryParameter("user_id"))
 assertTrue(JSONObject(patch.body.readUtf8()).getBoolean("done"))
 }}
 @Test fun rejectsDataOfAnotherAccount(){MockWebServer().use{s->s.start();s.enqueue(MockResponse().setBody("[{\"user_id\":\"other\"}]"));assertThrows(IllegalStateException::class.java){CloudApi(s.url("/"),"key").list("me","token","notes")}}}
 @Test fun failedConditionalDeleteIsNotAcknowledged(){MockWebServer().use{s->s.start();val e=Entry().apply{id="z";space="me";kind="notes";title="备忘";body="保留我";shadow=Wire.canon(this);hidden=true}
 s.enqueue(MockResponse().setBody("[]"));assertFalse(CloudApi(s.url("/"),"key").mutate("me","token",e));val req=s.takeRequest();assertEquals("DELETE",req.method);assertNull(req.requestUrl!!.queryParameter("content"));assertEquals("eq.0",req.requestUrl!!.queryParameter("updated_at"))}}
 @Test fun all241RowsReachNotebookWithoutRepeatingFirstPage(){MockWebServer().use{s->
  val rows=(0 until 241).map{i-> JSONObject().put("id","r"+i.toString().padStart(3,'0')).put("user_id","one").put("text","fixture").put("done",i<232).put("pinned",false).put("position",i).put("created_at",1).put("completed_at",JSONObject.NULL)}
  s.dispatcher=object:Dispatcher(){override fun dispatch(request:RecordedRequest):MockResponse{val u=request.requestUrl!!;val v=u.queryParameter("id")?.removePrefix("gt.")?:"";return MockResponse().setBody(org.json.JSONArray(rows.filter{it.getString("id")>v}.take(200)).toString())}}
  s.start();val data=CloudApi(s.url("/"),"fixture").list("one","test","todos");assertEquals(241,data.size);assertEquals(232,data.values.count{it.done});assertEquals(2,s.requestCount)
 }}
}