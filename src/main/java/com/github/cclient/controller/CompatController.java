package com.github.cclient.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.cclient.config.CompatConfiguration;
import com.github.cclient.index.dispatch.IndexDispatch;
import com.github.cclient.index.dispatch.IndexStringInfo;
import com.github.cclient.utils.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
@ResponseBody
@RequestMapping("/")
@Slf4j
public class CompatController {

    @Autowired
    private IndexDispatch indexDispatch;
    @Autowired
    private CompatConfiguration compatConfiguration;

    @ExceptionHandler({RuntimeException.class})
    public String exception(RuntimeException e) {
        log.error(e.getMessage());
        return "{\n" +
                "    \"error\": {\n" +
                "        \"root_cause\": [\n" +
                "            {\n" +
                "                \"type\": \"custom_gateway\",\n" +
                "                \"reason\": \"" + e.getMessage() + "\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"type\": \"custom_gateway\",\n" +
                "        \"reason\": \"" + e.getMessage() + "\"\n" +
                "    },\n" +
                "    \"status\": 407\n" +
                "}";
    }

    /***
     * 以7.10.2的规范接收请求，不含_type 或_type为 _doc
     * scroll本身就支持，不用特殊处理
     * @param indexes
     * @param params
     * @return
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET},
            path = "{indexes}/_search",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Object search(@PathVariable(name = "indexes", required = false) String indexes, @RequestParam(required = false) Map<String, String> params, @RequestBody(required = false) String body, ServerHttpRequest request) throws IOException {
        String reqUrl = indexDispatch.formatIndexs(indexes, null, params);
        log.info("request url format es7:" + reqUrl);
        return HttpUtil.post(reqUrl, body, compatConfiguration.getEs7Auth(), compatConfiguration.getIsEs7SSL());
    }

    /***
     * 以6.8.14的规范接收请求，含_type 且_type！=_doc，主要目标为展开_*为具体的_type
     * 兼容部分旧的es请求
     * filebeat_202101_*,filebeat_202012_*\/log,metrics/_search
     * 转换为 filebeat_202101_log,filebeat_202101_metrics,o:filebeat_202012_log,o:filebeat_202012_metrics/_search
     * @param indexes
     * @param params
     * @return
     */
    @RequestMapping(method = {RequestMethod.POST, RequestMethod.GET},
            path = "{indexes}/{types}/_search",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Object search(@PathVariable(name = "indexes", required = false) String indexes, @PathVariable("types") String types, @RequestParam(required = false) Map<String, String> params, @RequestBody(required = false) String body, ServerHttpRequest request) throws IOException {
        String reqUrl = indexDispatch.formatIndexs(indexes, types, params);
        log.info("request url format es6:" + reqUrl);
        return HttpUtil.post(reqUrl, body, compatConfiguration.getEs7Auth(), compatConfiguration.getIsEs7SSL());
    }

    /***
     * @param requestBody
     * @param params
     * @return
     * @throws IOException
     */
    @PostMapping(
            path = "/_bulk",
            consumes = MediaType.APPLICATION_NDJSON_VALUE, //spring-boot 2.4.4
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public String bulk(@RequestBody String requestBody, @RequestParam(required = false) Map<String, String> params) throws IOException {
        String urlParamsString = String.join("&", params.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.toList()));
        InputStream inputStream = new ByteArrayInputStream(requestBody.getBytes(UTF_8));
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, UTF_8);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        boolean nextLineIsOperaTarget = true;
        List<String> bulkToEs7 = new ArrayList<>(2000);
        List<String> bulkToEs6 = new ArrayList<>(2000);
        boolean isEs6 = false;
        String preLine = null;
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        while (reader.ready()) {
            String line = reader.readLine();
            //第一行必然为operaTarget
            if (nextLineIsOperaTarget) {
                JsonNode jsonNode = objectMapper.readTree(line);
                //其实只会有一个field,直接取next即可,我们也只关注"_index","_type"(如果存在的话)
                Map.Entry<String, JsonNode> field = jsonNode.fields().next();
                String operaName = field.getKey();
                //判断是否es6
                JsonNode targetPoint = field.getValue();
                String index = targetPoint.get("_index").asText();
                IndexStringInfo indexStringInfo = new IndexStringInfo(index);
                indexStringInfo.loadIndex(compatConfiguration.getIndexSplitBy(), compatConfiguration.getDateBoundary());
                isEs6 = indexDispatch.checkIndexIsV6(index);
                String id = targetPoint.get("_id").asText();
                if (isEs6) {
                    String type = CompatConfiguration.DEFAULT_TYPE;
                    if (compatConfiguration.getIsExtraType()) {
                        type = indexStringInfo.getType();
                    }
                    // if _index=filebeat_202103_log  >> _type=log
                    line = "{ \"" + operaName + "\" : { \"_index\" : \"" + index + "\", \"_type\" : \"" + type + "\", \"_id\" : \"" + id + "\" } }";
                } else {
                    line = "{ \"" + operaName + "\" : { \"_index\" : \"" + index + "\", \"_id\" : \"" + id + "\" } }";
                }
                if ("delete".equals(operaName)) {
                    nextLineIsOperaTarget = true;
                    //delete操作只存在操作符 { "delete" : { "_index" : "test", "_id" : "2" } }
                    if (isEs6) {
                        bulkToEs6.add(line);
                    } else {
                        bulkToEs7.add(line);
                    }
                } else {
                    preLine = line;
                    nextLineIsOperaTarget = false;
                    continue;
                }
            } else {
                //非delete操作同时存在操作符和操作数
                //操作符 例{ "update" : {"_id" : "1", "_index" : "test"} }
                //操作数 例{ "doc" : {"field2" : "value2"} }
                if (isEs6) {
                    bulkToEs6.add(preLine);
                    bulkToEs6.add(line);
                } else {
                    bulkToEs7.add(preLine);
                    bulkToEs7.add(line);
                }
                nextLineIsOperaTarget = true;
            }
        }
        if (bulkToEs6.size() == 0 && bulkToEs7.size() == 0) {
            return "{\"took\": 30,\"errors\": false}";
        }
        String es6Response = null;
        String es7Response = null;
        if (bulkToEs6.size() > 0) {
            es6Response = doPostBulk(compatConfiguration.getEs6Uri() + "/_bulk", urlParamsString, bulkToEs6, compatConfiguration.getEs6Auth(), compatConfiguration.getIsEs6SSL());
            if (bulkToEs7.size() == 0) {
                return es6Response;
            }
        }
        if (bulkToEs7.size() > 0) {
            es7Response = doPostBulk(compatConfiguration.getEs7Uri() + "/_bulk", urlParamsString, bulkToEs7, compatConfiguration.getEs7Auth(), compatConfiguration.getIsEs7SSL());
            if (bulkToEs6.size() == 0) {
                return es7Response;
            }
        }
        return mergeEsBulkResponse(objectMapper, es7Response, es6Response);
    }


    private String doPostBulk(String uri, String urlParamsString, List<String> lines, String auth, boolean isSSL) throws IOException {
        if (!urlParamsString.isEmpty()) {
            uri = uri + "?" + urlParamsString;
        }
        lines.add("");
        String esResponse = HttpUtil.post(uri, String.join("\n", lines), auth, isSSL);
        return esResponse;
    }

    /**
     * merge bulk2es6 response and bulk2es7 response
     * took use sum(es6res[took],es7res[took]),sum/avg/min/max
     *
     * @param objectMapper
     * @param es7Response
     * @param es6Response
     * @return
     * @throws IOException
     */
    private String mergeEsBulkResponse(ObjectMapper objectMapper, String es7Response, String es6Response) throws IOException {
        JsonNode es7ResponseJson = objectMapper.readTree(es7Response);
        JsonNode es6ResponseJson = objectMapper.readTree(es6Response);
        if (es7ResponseJson.get("errors").asBoolean() || es6ResponseJson.get("errors").asBoolean()) {
            ((ObjectNode) es7ResponseJson).set("errors", BooleanNode.getTrue());
        }
        ((ObjectNode) es7ResponseJson).set("took", IntNode.valueOf(es7ResponseJson.get("took").asInt() + es6ResponseJson.get("took").asInt()));
        es6ResponseJson.get("items").elements().forEachRemaining(item -> ((ArrayNode) (es7ResponseJson.get("items"))).add(item));
        return es7ResponseJson.toString();
    }
}
