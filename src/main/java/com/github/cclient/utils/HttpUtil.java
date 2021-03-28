package com.github.cclient.utils;

import okhttp3.*;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
/**
 * @author cclient
 */
public class HttpUtil {
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    public static final MediaType NDJSON
            = MediaType.get("application/x-ndjson; charset=utf-8");

    static private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final SSLSocketFactory sslSocketFactory = SSLUtil.sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) SSLUtil.trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(String url,String json,String auth,boolean isSSL) throws IOException {
        OkHttpClient client;
        if (isSSL){
            client = getUnsafeOkHttpClient();
        }else{
            client = new OkHttpClient();
        }
        Request.Builder builder= new Request.Builder().url(url);
        if(json!=null&&!json.isEmpty()){
            RequestBody body = RequestBody.create(json,JSON);
            builder=builder.post(body);
        }
        if(auth!=null&&!auth.isEmpty()){
            builder=builder.header("Authorization",auth);
        }
        Request request = builder.build();
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }
}
