package com.lg.example.httpclient.fundamentals;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by liuguo on 2016/11/25.
 */
public class HttpContextExample {

    /**
     * 不实用上下文但是使用同一个CloseableHttpClient实例
     * 会返回相同的JSESSIONID=E35337C59C4EB899CF803081C9174510
     * @throws IOException
     */
    @Test
    public void testNoHttpContextSameClient() throws IOException{
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(1000)
                .build();

        HttpGet httpget1 = new HttpGet("http://localhost:8080/login?username=lg");
        httpget1.setConfig(requestConfig);
        CloseableHttpResponse response1 = httpclient.execute(httpget1);
        System.out.println(response1);
        try {
            HttpEntity entity1 = response1.getEntity();
            System.out.println(EntityUtils.toString(entity1));
        } finally {
            response1.close();
        }
        HttpGet httpget2 = new HttpGet("http://localhost:8080/login?username=lg");
        CloseableHttpResponse response2 = httpclient.execute(httpget2);
        System.out.println(response2);
        try {
            HttpEntity entity2 = response2.getEntity();
            System.out.println(EntityUtils.toString(entity2));
        } finally {
            response2.close();
        }
        //login success
        //you have been logined lg
    }

    /**
     * 不使用上下文且使用不同的CloseableHttpClient实例
     * 返回两个不同的
     *  JSESSIONID=0A1579FA564838B089ADCE3F4D21D731
     *  JSESSIONID=AC5E9C4AB6E7513929B68C7BD36DE59A
     * @throws IOException
     */
    @Test
    public void testNoHttpContext() throws IOException{
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8080/login?username=lg");
        CloseableHttpResponse response1 = closeableHttpClient.execute(httpGet);
        System.out.println(response1);
        try {
            HttpEntity entity1 = response1.getEntity();
            if(entity1!=null){
                System.out.println(EntityUtils.toString(entity1));
            }
            EntityUtils.consume(entity1);

        } finally {
            response1.close();
            closeableHttpClient.close();
        }

        CloseableHttpClient closeableHttpClient2 = HttpClients.createDefault();
        HttpGet httpget2 = new HttpGet("http://localhost:8080/login?username=lg");
        CloseableHttpResponse response2 = closeableHttpClient2.execute(httpget2);
        System.out.println(response2);
        try {
            HttpEntity entity2 = response2.getEntity();
            if(entity2!=null){
                System.out.println(EntityUtils.toString(entity2));
            }
            EntityUtils.consume(entity2);

        } finally {
            response2.close();
            closeableHttpClient2.close();
        }

        /**
         *  HttpResponseProxy{HTTP/1.1 200 OK [Server: Apache-Coyote/1.1, Set-Cookie: JSESSIONID=0A1579FA564838B089ADCE3F4D21D731; Path=/; HttpOnly, Content-Type: text/plain;charset=UTF-8, Content-Length: 13, Date: Fri, 25 Nov 2016 06:08:41 GMT] ResponseEntityProxy{[Content-Type: text/plain;charset=UTF-8,Content-Length: 13,Chunked: false]}}
            login success
             Disconnected from the target VM, address: '127.0.0.1:63498', transport: 'socket'
             HttpResponseProxy{HTTP/1.1 200 OK [Server: Apache-Coyote/1.1, Set-Cookie: JSESSIONID=AC5E9C4AB6E7513929B68C7BD36DE59A; Path=/; HttpOnly, Content-Type: text/plain;charset=UTF-8, Content-Length: 13, Date: Fri, 25 Nov 2016 06:08:41 GMT] ResponseEntityProxy{[Content-Type: text/plain;charset=UTF-8,Content-Length: 13,Chunked: false]}}
             login success
         */
    }

    /**
     * 使用上下文且使用不同的CloseableHttpClient实例
     * JSESSIONID被记录下来了.
     * BasicHttpContext里有个Map<String,Object>的对象用来记录一次请求响应的信息，当响应信息返回时，就会被set到context里，
     * 当然响应的cookie信息也就被存储在context里,包括传回的sessionId。
     * 当第二次请求的时候传入相同的context，那么请求的过程中会将context里的sessionId提取出来传给服务器，sessionId一样，自然而然的就是同一个session对象
     * @throws IOException
     */
    @Test
    public void testHttpContext() throws IOException{
        HttpContext context = new BasicHttpContext();
        HttpClientContext clientContext = HttpClientContext.adapt(context);

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(1000)
                .build();

        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost:8080/login?username=lg");
        httpGet.setConfig(requestConfig);
        CloseableHttpResponse response1 = closeableHttpClient.execute(httpGet,clientContext);
        System.out.println(clientContext.getRequestConfig());
        System.out.println(clientContext.getResponse());
        System.out.println(clientContext.getHttpRoute());
        System.out.println("*****************************");
        try {
            HttpEntity entity1 = response1.getEntity();
            if(entity1!=null){
                System.out.println(EntityUtils.toString(entity1));
            }
            EntityUtils.consume(entity1);

        } finally {
            response1.close();
            closeableHttpClient.close();
        }

        CloseableHttpClient closeableHttpClient2 = HttpClients.createDefault();
        HttpGet httpget2 = new HttpGet("http://localhost:8080/login?username=lg");
        CloseableHttpResponse response2 = closeableHttpClient2.execute(httpget2,clientContext);

        try {
            HttpEntity entity2 = response2.getEntity();
            if(entity2!=null){
                System.out.println(EntityUtils.toString(entity2));
            }
            EntityUtils.consume(entity2);

        } finally {
            response2.close();
            closeableHttpClient2.close();
        }
        System.out.println("*****************************");
        System.out.println(clientContext.getRequestConfig());
        System.out.println(clientContext.getResponse());
        System.out.println(clientContext.getHttpRoute());
        /**
         *  [expectContinueEnabled=false, proxy=null, localAddress=null, cookieSpec=null, redirectsEnabled=true, relativeRedirectsAllowed=true, maxRedirects=50, circularRedirectsAllowed=false, authenticationEnabled=true, targetPreferredAuthSchemes=null, proxyPreferredAuthSchemes=null, connectionRequestTimeout=-1, connectTimeout=1000, socketTimeout=1000, contentCompressionEnabled=true]
             HttpResponseProxy{HTTP/1.1 200 OK [Server: Apache-Coyote/1.1, Set-Cookie: JSESSIONID=8176AE23A84A0F4541249B750B71578C; Path=/; HttpOnly, Content-Type: text/plain;charset=UTF-8, Content-Length: 13, Date: Fri, 25 Nov 2016 05:30:53 GMT] ResponseEntityProxy{[Content-Type: text/plain;charset=UTF-8,Content-Length: 13,Chunked: false]}}
             {}->http://localhost:8080
             *****************************
             login success
             you have been logined lg
             *****************************
             [expectContinueEnabled=false, proxy=null, localAddress=null, cookieSpec=null, redirectsEnabled=true, relativeRedirectsAllowed=true, maxRedirects=50, circularRedirectsAllowed=false, authenticationEnabled=true, targetPreferredAuthSchemes=null, proxyPreferredAuthSchemes=null, connectionRequestTimeout=-1, connectTimeout=1000, socketTimeout=1000, contentCompressionEnabled=true]
             HttpResponseProxy{HTTP/1.1 200 OK [Server: Apache-Coyote/1.1, Content-Type: text/plain;charset=UTF-8, Content-Length: 24, Date: Fri, 25 Nov 2016 05:30:53 GMT] ResponseEntityProxy{[Content-Type: text/plain;charset=UTF-8,Content-Length: 24,Chunked: false]}}
             {}->http://localhost:8080
         */
    }
}
