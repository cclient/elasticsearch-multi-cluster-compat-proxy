package com.github.cclient.cache;

import com.github.cclient.entity.IndexInfo;
import com.github.cclient.repository.IndexInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author cclient
 */
@Service
@Slf4j
public class IndexInfoCache {

    private static ConcurrentHashMap<String, IndexInfo> cacheMap = new ConcurrentHashMap<>();

    @Autowired
    IndexInfoRepository repository;

    public static IndexInfo getCache(String index) {
        if (cacheMap.containsKey(index)) {
            cacheMap.get(index);
        }
        return null;
    }

    public Integer reFreshCaches() {
        synchronized (this) {
            Iterable<IndexInfo> indexInfos = repository.findAll();
            ConcurrentHashMap<String, IndexInfo> tmpCacheMap = new ConcurrentHashMap<>(204800);
            indexInfos.forEach(indexInfo -> tmpCacheMap.put(indexInfo.getIndex(), indexInfo));
            log.info("refresh: " + tmpCacheMap.size());
            cacheMap = tmpCacheMap;
            return tmpCacheMap.size();
        }
    }
}
