package com.github.cclient.index.dispatch;

import com.github.cclient.cache.IndexInfoCache;
import com.github.cclient.config.CompatConfiguration;
import com.github.cclient.entity.IndexInfo;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class IndexDispatch {
    @Autowired
    CompatConfiguration compatConfiguration;

    public String formatIndexsV7(String indexes) {
        String[] indexArr = indexes.split(",");
        List<String> newIndexList = new ArrayList<>(indexArr.length);
        for (int i = 0; i < indexArr.length; i++) {
            String index = indexArr[i];
            String newIndexName = formatIndexName(index);
            newIndexList.add(newIndexName);
        }
        String urlIndexString = String.join(",", newIndexList);
        return urlIndexString;
    }


    public String formatIndexName(String index) {
        String newIndexName = formatIndexNameByMysql(index);
        if (newIndexName == null) {
            newIndexName = formatIndexNameByString(index);
        }
        return newIndexName;
    }


    public String formatIndexNameByString(String index) {
        IndexStringInfo indexStringInfo = new IndexStringInfo(index);
        indexStringInfo.loadIndex(compatConfiguration.getIndexSplitBy(), compatConfiguration.getDateBoundary());
        String destIndex = indexStringInfo.getDestIndex(compatConfiguration.getRemoteClusterName());
        return destIndex;
    }

    public String formatIndexNameByMysql(String index) {
        IndexInfo indexInfo = IndexInfoCache.getCache(index);
        if (indexInfo == null) {
            return null;
        }
        if (indexInfo.getRemoteClusterName() != null && !indexInfo.getRemoteClusterName().isEmpty()) {
            return indexInfo.getRemoteClusterName() + ":" + indexInfo.getDestIndex();
        } else {
            return indexInfo.getDestIndex();
        }
    }

    public boolean checkIndexIsV6(String index) {
        IndexInfo indexInfo = IndexInfoCache.getCache(index);
        if (indexInfo == null) {
            IndexStringInfo indexStringInfo = new IndexStringInfo(index);
            indexStringInfo.loadIndex(compatConfiguration.getIndexSplitBy(), compatConfiguration.getDateBoundary());
            return indexStringInfo.getVersion() == 6;
        } else {
            return (indexInfo.getRemoteClusterName() != null && !indexInfo.getRemoteClusterName().isEmpty()) || (indexInfo.getVersion() != null && indexInfo.getVersion() == 6);
        }
    }

    public String formatIndexsV6ToV7(String indexes, String types) {
        var indexArr = indexes.split(",");
        var typeArr = types.split(",");
        List<String> newIndexList = new ArrayList<>(indexArr.length);
        for (int i = 0; i < indexArr.length; i++) {
            String index = indexArr[i];
            List<String> subIndexex = formatV6UrlToV7Single(index, typeArr);
            newIndexList.addAll(subIndexex);
        }
        String urlIndexString = String.join(",", newIndexList);
        return urlIndexString;
    }

    public List<String> formatV6UrlToV7Single(String index, String[] typeArr) {
        var indexSubPartArray = index.split(compatConfiguration.getIndexSplitBy());
        List<String> newIndexes = new ArrayList<>(typeArr.length);
        String type;
        if (indexSubPartArray.length > 2) {
//                filebeat_202103_log
            type = indexSubPartArray[indexSubPartArray.length - 1];
            if (type.equals("*")) {
                for (String s : typeArr) {
                    String newIndex = String.join(compatConfiguration.getIndexSplitBy(), Arrays.asList(indexSubPartArray).subList(0, indexSubPartArray.length - 1)) + compatConfiguration.getIndexSplitBy() + s;
                    newIndexes.add(formatIndexName(newIndex));
                }
            } else {
                newIndexes.add(formatIndexName(index));
            }
        } else if (indexSubPartArray.length > 1) {
//                filebeat_202103
            newIndexes.add(formatIndexName(index));
        } else {
//                filebeat
            newIndexes.add(index);
        }
        return newIndexes;
    }

    public String formatIndexs(String indexes, String types, Map<String, String> params) {
        String urlIndexString = indexes;
        if (!indexes.isEmpty()) {
            if (types == null || "_doc".equals(types)) {
                urlIndexString = formatIndexsV7(indexes);
            } else {
                urlIndexString = formatIndexsV6ToV7(indexes, types);
            }
        }
        String urlParamsString = String.join("&", params.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.toList()));
        log.debug("old indexs:" + indexes);
        log.debug("new indexs:" + urlIndexString);
        log.debug("old parms:" + urlParamsString);
        String newUrl = urlIndexString + "/_search";
        if (!urlParamsString.isEmpty()) {
            newUrl = newUrl + "?" + urlParamsString;
        }
        String reqUrl = compatConfiguration.getEs7Uri() + "/" + newUrl;
        return reqUrl;
    }
}
