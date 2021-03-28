package com.github.cclient.index.dispatch;

import com.github.cclient.cache.IndexInfoCache;
import com.github.cclient.config.CompatConfiguration;
import com.github.cclient.entity.IndexInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndexDispatch {
    @Autowired
    CompatConfiguration compatConfiguration;
    public String formatIndexs(String indexes){
        String[] indexArr=indexes.split(",");
        List<String> newIndexList=new ArrayList<>(indexArr.length);
        for (int i = 0; i < indexArr.length; i++) {
            String index=indexArr[i];
            String newIndexName=formatIndexNameByMysql(index);
            if(newIndexName==null){
                newIndexName=formatIndexNameByString(index);
            }
            newIndexList.add(newIndexName);
        }
        String urlIndexString=String.join(",",newIndexList);
        return urlIndexString;
    }

    public String formatIndexNameByString(String index) {
        IndexStringInfo indexStringInfo =new IndexStringInfo(index);
        indexStringInfo.loadIndex(compatConfiguration.getIndexSplitBy(),compatConfiguration.getDateBoundary());
        String destIndex= indexStringInfo.getDestIndex(compatConfiguration.getRemoteClusterName());
        return destIndex;
    }
    public String formatIndexNameByMysql(String index) {
        IndexInfo indexInfo= IndexInfoCache.getCache(index);
        if(indexInfo==null){
            return null;
        }
        if(indexInfo.getRemoteClusterName()!=null&&!indexInfo.getRemoteClusterName().isEmpty()){
            return indexInfo.getRemoteClusterName()+":"+indexInfo.getDestIndex();
        }else{
            return indexInfo.getDestIndex();
        }
    }
}
