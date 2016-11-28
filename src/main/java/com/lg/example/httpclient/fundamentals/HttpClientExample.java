package com.lg.example.httpclient.fundamentals;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import junit.framework.TestCase;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by liuguo on 2016/11/22.
 */
public class HttpClientExample extends TestCase{

    public static String httpGet(String url){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        StringBuilder sb = new StringBuilder();
        try {
            CloseableHttpResponse response =  httpClient.execute(httpGet);
            System.out.println("StatusLine:"+response.getStatusLine());
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String s;
            while ((s = br.readLine())!=null){
                sb.append(s);
            }
            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * The difference between closing the content stream and closing the response is that
     * the former will attempt to keep the underlying connection alive by consuming the entity content
     * while the latter immediately shuts down and discards the connection
     * 关闭内容流和关闭响应的区别是前者试图通过消耗响应体内容来保持底层连接活跃而后者是立即关闭并释放连接.
     * note that the HttpEntity#writeTo(OutputStream) method is also required to ensure proper release of system resources
     * once the entity has been fully written out. If this method obtains an instance of java.io.InputStream by calling HttpEntity#getContent(),
     * it is also expected to close the stream in a finally clause.
     * @param url
     * @param map
     * @return
     * @throws IOException
     */
    public static String httpPost(String url, Map<String,String> map) throws IOException{
        StringBuilder sb = new StringBuilder();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        for(String key : map.keySet()){
            nameValuePairs.add(new BasicNameValuePair(key,map.get(key)));
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            // The underlying HTTP connection is still held by the response object
            // to allow the response content to be streamed directly from the network socket.
            // In order to ensure correct deallocation of system resources
            // the user MUST call CloseableHttpResponse#close() from a finally clause.
            // Please note that if response content is not fully consumed the underlying
            // connection cannot be safely re-used and will be shut down and discarded
            // by the connection manager
            try {
                System.out.println("StatusLine:"+response.getStatusLine());
                HttpEntity entity = response.getEntity();
                // System.out.println("entity="+EntityUtils.toString(entity));
                //EntityUtils.toString(entity) 等价于下面的.
                if(entity!=null){
                    InputStream is = entity.getContent();
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
                        String s;
                        while ((s=br.readLine())!=null){
                            sb.append(s);
                        }
                    } finally {
                        is.close(); //关闭内容流
                    }
                }
                //EntityUtils.consume(entity);//当使用streaming entities时,这里相当于上面的关闭流
            } finally {
                response.close(); //关闭http连接
            }

        }finally{
            httpClient.close();
        }

        return sb.toString();
    }

    @Test
    public void testBuildURI() throws Exception{

        URI uri = new URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPath("/tpp/rest/publish/queryCount.json")
                .addParameter("key", "pubcount")
                .addParameter("beginTime", "2016-11-16")
                .addParameter("endTime", "2016-11-16")
                .build();

        System.out.println(uri.toString());
        HttpGet httpGet = new HttpGet(uri);
        System.out.println(httpGet.getURI());
    }

    @Test
    public void testHttpGet(){
        String url = "http://localhost/tpp/rest/zhihui/publish/queryCount.json?key=pubcount&beginTime=2016-11-16&endTime=2016-11-16";
        httpGet(url);
    }
    @Test
    public void testHttpPost() throws Exception{
        String url = "http://localhost/tpp/rest/auth/login?token=1d19e6282781376d665cd052172377a1&siteCode=S1";
        Map<String,String> map = new HashMap<String, String>();
        map.put("authorization","ocean:2ffc14b6e2f9f699a3addc90d9bae15a:S1");

        System.out.println(httpPost(url,map));
    }

    @Test
    public void testResponse(){
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK,"OK");
        System.out.println(response.getProtocolVersion());
        System.out.println(response.getStatusLine().getStatusCode());
        System.out.println(response.getStatusLine().getReasonPhrase());
        System.out.println(response.getStatusLine().toString());
    }

    @Test
    public void testHeaders(){
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_OK,"OK");
        response.addHeader("Set-Cookie","c1=a; path=/; domain=localhost");
        response.addHeader("Set-Cookie","c2=b; path=\"/\", c3=c; domain=\"localhost\"");

        Header h1 = response.getFirstHeader("Set-Cookie");
        System.out.println(h1);
        System.out.println("**********************");
        for(HeaderElement element : h1.getElements()){
            System.out.println(element.getName()+": "+element.getValue());
        }
        System.out.println("**********************");
        Header h2 = response.getLastHeader("Set-Cookie");
        System.out.println(h2);
        System.out.println("**********************");
        Header[] headers = response.getHeaders("Set-Cookie");
        System.out.println(Arrays.toString(headers));
        System.out.println("**********************");
        //The most efficient way to obtain all headers of a given type is by using the HeaderIterator interface
        HeaderIterator it = response.headerIterator("Set-Cookie");
        while (it.hasNext()){
            System.out.println(it.next());
        }
    }

    @Test
    public void testHeaderMessage(){
        HttpResponse response = new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_OK,"OK");
        response.addHeader("Set-Cookie","c1=a; path=/; domain=localhost");
        response.addHeader("Set-Cookie","c2=b; path=\"/\", c3=c; domain=\"localhost\"");

        HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));

        while (it.hasNext()){
            HeaderElement element = it.nextElement();
            System.out.println(element.getName()+":"+element.getValue());
            NameValuePair[] pairs = element.getParameters();
            for(NameValuePair pair : pairs){
                System.out.println(pair.getName()+"=="+pair.getValue());
            }
            System.out.println("************");
        }
    }

    /**
     * EntityUtils.toString(entity) 有长度限制
     * the use of EntityUtils is strongly discouraged unless the response entities originate from a trusted HTTP server
     * and are known to be of limited length
     *
     * @throws IOException
     */
    @Test
    public void testEntity() throws IOException{
        StringEntity entity = new StringEntity("import message", ContentType.create("text/plain","utf-8"));
        System.out.println(entity.getContentType());
        System.out.println(entity.getContentLength());
        System.out.println(EntityUtils.toString(entity));
        System.out.println(EntityUtils.toByteArray(entity).length);
    }

    /**
     * when only a small portion of the entire response content needs to be retrieved and
     * the performance penalty for consuming the remaining content and making the connection reusable is too high,
     * in which case one can terminate the content stream by closing the response.
     * The connection will not be reused, but all level resources held by it will be correctly deallocated
     * @throws IOException
     */
    @Test
    public void testPortionOfContent() throws IOException{
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost/");
        CloseableHttpResponse response =  httpClient.execute(httpGet);

        try {
            HttpEntity entity =  response.getEntity();
            if(entity!=null){

                InputStream is = entity.getContent();
                int a =  is.read();
                int b = is.read();
                //剩下的不需要
            }
        } finally {
            response.close();
        }
    }

    /**
     * 可重复读取的内容流
     * In some situations it may be necessary to be able to read entity content more than once.
     * In this case entity content must be buffered in some way, either in memory or on disk.
     * The simplest way to accomplish that is by wrapping the original entity with the BufferedHttpEntity class.
     * This will cause the content of the original entity to be read into a in-memory buffer.
     * In all other ways the entity wrapper will be have the original one
     * @throws Exception
     */
    @Test
    public void testBufferedEntity() throws Exception{
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://localhost/");
        CloseableHttpResponse response =  closeableHttpClient.execute(httpGet);

        try {
            HttpEntity entity = response.getEntity();
            if(entity!=null){
                //EntityUtils.toString(entity);
                //EntityUtils.toString(entity);//这里会报java.io.IOException: Attempted read from closed stream.因为连接已经关闭
                entity = new BufferedHttpEntity(entity);
                EntityUtils.toString(entity); //ok
                EntityUtils.toString(entity); //ok
            }
        } finally {
            response.close();
        }
    }

    /**
     * 生成请求体
     * @throws IOException
     */
    @Test
    public void testProducingEntity() throws IOException{
        File file = new File("./data.txt");
        FileEntity fileEntity = new FileEntity(file,ContentType.create("text/plain", "UTF-8"));
        System.out.println(EntityUtils.toString(fileEntity));
        System.out.println(EntityUtils.toString(fileEntity));
        //note InputStreamEntity is not repeatable, because it can only read from the underlying data stream once.
        // Generally it is recommended to implement a custom HttpEntity class which is self-contained instead of
        // using the generic InputStreamEntity. FileEntity can be a good starting point
        InputStreamEntity inputStreamEntity = new InputStreamEntity(new FileInputStream(file));
        System.out.println(EntityUtils.toString(inputStreamEntity,"UTF-8"));
        //System.out.println(EntityUtils.toString(inputStreamEntity,"UTF-8")); //java.io.IOException: Stream Closed
    }

    /**
     * 模拟html form表单
     */
    @Test
    public void testHtmlForms() throws Exception{
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("username","lg"));
        nameValuePairs.add(new BasicNameValuePair("password","heheh"));

        UrlEncodedFormEntity encodedFormEntity = new UrlEncodedFormEntity(nameValuePairs,Consts.UTF_8);
        HttpPost httpPost = new HttpPost("http://localhost/login");
        httpPost.setEntity(encodedFormEntity);

        System.out.println(EntityUtils.toString(httpPost.getEntity()));//username=lg&password=heheh
    }

    /**
     * http chunk编码
     * @throws IOException
     */
    @Test
    public void testContentChunk() throws IOException{
        StringEntity stringEntity = new StringEntity("import message",ContentType.create("text/plain","UTF-8"));
        stringEntity.setChunked(true);//This value will be ignored when using HTTP protocol versions that do not support chunk coding, such as HTTP/1.0

        HttpPost httpPost = new HttpPost("http://localhost/messsage");
        httpPost.setEntity(stringEntity);

        System.out.println(EntityUtils.toString(httpPost.getEntity()));
    }

    /**
     * The simplest and the most convenient way to handle responses is by using the ResponseHandler interface,
     * which includes the handleResponse(HttpResponse response) method.
     * This method completely relieves the user from having to worry about connection management.
     * When using a ResponseHandler, HttpClient will automatically take care of ensuring release of the connection
     * back to the connection manager regardless whether the request execution succeeds or causes an exception
     *
     * 使用ResponseHander#handleResponse来处理响应结果可以让开发者不再担心连接管理.
     * 当使用ResponseHandler时，HttpClient无论请求成功或是遇到异常都将自动保证关闭连接资源.
     * @throws IOException
     */
    @Test
    public void testResponseHander() throws IOException{
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet("http://api.map.baidu.com/place/v2/search?query=银行&page_size=10&page_num=0&scope=1&region=成都&output=json&ak=b8q6flHsDqtpREDsluXYLxB8jstp0bN5");

        ResponseHandler<JSONObject> rh = new ResponseHandler<JSONObject>() {
            public JSONObject handleResponse(final HttpResponse httpResponse) throws IOException {
                StatusLine statusLine = httpResponse.getStatusLine();
                if(statusLine.getStatusCode() >= 300){
                    throw new HttpResponseException(
                            statusLine.getStatusCode(),
                            statusLine.getReasonPhrase());
                }
                HttpEntity entity = httpResponse.getEntity();
                if(entity == null){
                    throw new ClientProtocolException("Response contains no content");
                }

                ContentType contentType = ContentType.getOrDefault(entity);
                Charset charset = contentType.getCharset();
                Reader reader = new InputStreamReader(entity.getContent(),charset);
                return JSON.parseObject(entity.getContent(),charset, Object.class, Feature.AutoCloseSource);
            }
        };
        JSONObject myjson = closeableHttpClient.execute(httpget, rh);
        System.out.println(JSONObject.toJSONString(myjson));
    }

    /**
     * 自定义HttpClient
     * HttpClient interface represents the most essential contract for HTTP request execution.
     * It imposes no restrictions or particular details on the request execution process
     * and leaves the specifics of connection management, state management, authentication
     * and redirect handling up to individual implementations.
     * This should make it easier to decorate the interface with additional functionality
     * such as response content caching.
     * Generally HttpClient implementations act as a facade to a number of special purpose handler
     * or strategy interface implementations responsible for handling of a particular aspect of the HTTP protocol
     * such as redirect or authentication handling or making decision about connection persistence
     * and keep alive duration. This enables the users to selectively replace default implementation of
     * those aspects with custom, application specific ones.
     */
    @Test
    public void testCustomHttpClient(){
        CloseableHttpClient closeableHttpClient = HttpClients
                .custom()
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                    @Override
                    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                        long keepAliveDuration = super.getKeepAliveDuration(response, context);
                        // Keep connections alive 5 seconds if a keep-alive value
                        // has not be explicitly set by the server
                        if (keepAliveDuration == -1) {
                            keepAliveDuration = 5000;
                        }
                        return keepAliveDuration;
                    }
                }).build();
    }

    @Test
    public void testRetryHandler(){

        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException e, int i, HttpContext httpContext) {
                if(i >= 5){
                    // Do not retry if over max retry count
                    return false;
                }

                if(e instanceof InterruptedIOException){
                    // Timeout
                    return false;
                }

                if(e instanceof UnknownHostException){
                    //unknown host
                    return false;
                }

                if(e instanceof ConnectTimeoutException){
                    // Connection refused
                    return false;
                }

                if(e instanceof SSLException){
                    // SSL handshake exception
                    return false;
                }

                HttpClientContext httpClientContext = HttpClientContext.adapt(httpContext);
                HttpRequest request =  httpClientContext.getRequest();
                //非封闭的请求幂等
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if(!idempotent){
                    // Retry if the request is considered idempotent
                    //重试如果这个请求是幂等的
                    return true;
                }
                return false;
            }
        };

        CloseableHttpClient closeableHttpClient = HttpClients
                .custom()
                .setRetryHandler(httpRequestRetryHandler)
                .build();
    }

    @Test
    public void testRedirectHandler(){
       /* LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRedirectStrategy(redirectStrategy)
                .build();*/

       CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpClientContext context = HttpClientContext.create();
        HttpGet httpGet = new HttpGet("http://localhost:8080/login");


    }






}
