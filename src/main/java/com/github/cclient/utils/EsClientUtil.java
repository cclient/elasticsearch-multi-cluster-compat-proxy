//package com.github.cclient.utils;
//
//import com.github.cclient.config.EsHost;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.http.HttpHost;
//import org.apache.http.message.BasicHeader;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestClientBuilder;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.action.bulk.BulkRequest;
//import org.elasticsearch.action.bulk.BulkResponse;
//import org.elasticsearch.action.delete.DeleteRequest;
//import org.elasticsearch.action.index.IndexRequest;
//import org.elasticsearch.action.update.UpdateRequest;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.common.Strings;
//import org.elasticsearch.common.xcontent.ToXContent;
//import org.elasticsearch.common.xcontent.json.JsonXContent;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//
//@Slf4j
//@Deprecated
//public class EsClientUtil {
//    public static RestHighLevelClient getClient(EsHost[] esHostList, String auth, boolean isSSL) {
//        HttpHost[] hosts = Arrays.asList(esHostList).stream().map(host -> new HttpHost(host.getHostname(),host.getPort(),host.getScheme())).toArray(HttpHost[]::new);
//        RestClientBuilder builder = RestClient.builder(hosts);
//        if(!auth.isEmpty()){
//            BasicHeader basicHeader=new BasicHeader("Authorization", auth);
//            builder.setDefaultHeaders(new BasicHeader[]{basicHeader});
//        }
//        if(isSSL){
//            builder.setHttpClientConfigCallback((clientBuilder) -> {
//                clientBuilder.setSSLContext(SSLUtil.sslContext);
//                clientBuilder.setSSLHostnameVerifier((hostname, session) -> true);
//                return clientBuilder;
//            });
//        }
//        RestHighLevelClient client = new RestHighLevelClient(builder);
//        return client;
//    }
//    /**
//     * RestHighLevelClient _bulk 需解析每行json，构造相应的IndexRequest/UpdateRequest/DeleteRequest 只放一个示例参考代码
//     * 未做测试和验证
//     * @param esHosts
//     * @param lines
//     * @param auth
//     * @param isSSL
//     * @return
//     * @throws IOException
//     */
//
//    public String doPostBulk(EsHost[] esHosts, List<String> lines, String auth, boolean isSSL) throws IOException {
//        RestHighLevelClient client=getClient(esHosts,auth,isSSL);
//        BulkRequest bulkRequest=new BulkRequest();
//        //index
//        IndexRequest indexRequest=new IndexRequest("index","type","id");
//        indexRequest.source(new HashMap<String,Object>(1){{
//            put("field1", "value1");
//        }});
//        //delete
//        DeleteRequest deleteRequest=new DeleteRequest("index","type","id");
//        //update
//        UpdateRequest updateRequest=new UpdateRequest("index","type","id");
//        updateRequest.doc(new HashMap<String,Object>(1){{
//            put("field2", "value2");
//        }});
//        bulkRequest.add(indexRequest);
//        bulkRequest.add(deleteRequest);
//        bulkRequest.add(updateRequest);
//        BulkResponse bulkResponse=client.bulk(bulkRequest,RequestOptions.DEFAULT);
//        return Strings.toString(bulkResponse.toXContent(JsonXContent.contentBuilder(), ToXContent.EMPTY_PARAMS).humanReadable(true));
//    }
//}