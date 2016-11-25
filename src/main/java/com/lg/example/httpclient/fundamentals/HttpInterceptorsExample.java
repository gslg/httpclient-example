package com.lg.example.httpclient.fundamentals;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by liuguo on 2016/11/25.
 */
public class HttpInterceptorsExample {

    @Test
    public void testInterceptors() throws IOException{
        final HttpClientContext httpClientContext = HttpClientContext.create();

        AtomicInteger count = new AtomicInteger(1);
        httpClientContext.setAttribute("Count",count);

        //请求拦截器
        HttpRequestInterceptor httpRequestInterceptor = new HttpRequestInterceptor() {
            public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
                AtomicInteger count = (AtomicInteger) httpContext.getAttribute("Count");

                httpRequest.addHeader("Count",String.valueOf(count.getAndIncrement()));
            }
        };

        //响应处理器
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                HttpEntity entity = httpResponse.getEntity();
                if(entity != null){
                    return EntityUtils.toString(entity);
                }
                return null;
            }
        };

        final CloseableHttpClient httpClient = HttpClients
                .custom()
                .addInterceptorLast(httpRequestInterceptor)
                .build();

        final HttpGet httpget = new HttpGet("http://localhost:8080/hello");

        for (int i = 0; i < 20; i++) {

           String result =  httpClient.execute(httpget,responseHandler,httpClientContext);
            System.out.println(result);

        }

    }
}
