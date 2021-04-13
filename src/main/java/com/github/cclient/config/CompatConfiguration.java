package com.github.cclient.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author santiiny
 * @date 2020/11/21 2:32 下午
 */
@Configuration
@Slf4j
@Data
public class CompatConfiguration {
    public static String DEFAULT_TYPE = "_doc";
    /**
     * 索引分隔符，例 filebeat_202103_log 分隔符为_
     */
    @Value("${es.compat.index.split-by}")
    private String indexSplitBy;
    /**
     * 索引日期分界
     * < 202101 elasticsearch 6.8
     * >=202101 elasticsearch 7.10
     */
    @Value("${es.compat.index.date-boundary}")
    private Integer dateBoundary;

    /**
     * es7.10内部注册es6.8 做为es7.10的remote.cluster,es 6的名称
     * https://www.elastic.co/guide/en/elasticsearch/reference/7.10/modules-remote-clusters.html
     */
    @Value("${es.compat.es7.remote-cluster-name}")
    private String remoteClusterName;

    /**
     * es7的http 访问地址
     * 用于分发_bulk请求
     */
    @Value("${es.compat.es7.uri}")
    private String es7Uri;

    @Value("${es.compat.es7.auth}")
    private String es7Auth;


    @Value("${es.compat.es7.ssl}")
    private Boolean isEs7SSL;

    @Value("${es.compat.es6.uri}")
    private String es6Uri;

    @Value("${es.compat.es6.auth}")
    private String es6Auth;

    @Value("${es.compat.es6.ssl}")
    private Boolean isEs6SSL;

    /**
     * 是否需要提取type
     * 对es6 因为还支持_type，不一定是_doc,因此需要从index提取出_type,直接从index提取，或从mysql等外部存储获取
     */
    @Value("${es.compat.es6.extra-type}")
    private Boolean isExtraType;

//    @PostConstruct
//    public void initRms() {
//        log.info("rmsConfiguration {} ", rmsConfiguration);
//        AbstractCompose.DEFAULT_NAMESPACE = rmsConfiguration.getNameSpace();
//        AbstractCompose.DEFAULT_IMAGE_PULLSECRET = rmsConfiguration.getSecret();
//        log.info("rmsConfiguration {} ", rmsConfiguration);
//    }
}
