package com.github.cclient.index.dispatch;

//import com.github.cclient.config.Conf;
import lombok.Data;
import lombok.var;

import java.util.Arrays;

@Data
public class IndexStringInfo {
    private String orginalIndexName;
    private String orginalIndexPre;
    private int version;
    private Integer dateNum;
    private String type;

    public IndexStringInfo(String orginalIndexName){
        this.orginalIndexName=orginalIndexName;
    }

    public void loadIndex(String indexSplitBy,int dateBoundary){
        var indexSubPartArray= orginalIndexName.split(indexSplitBy);
        if(indexSubPartArray.length>2){
            orginalIndexPre=String.join(indexSplitBy,Arrays.asList(indexSubPartArray).subList(0,indexSubPartArray.length-1));
//          filebeat_202103_log
            //202103
            dateNum=Integer.parseInt(indexSubPartArray[indexSubPartArray.length-2]) ;
            //log
            type=indexSubPartArray[indexSubPartArray.length-1];
            if(dateNum<dateBoundary){
                version=6;
            }else{
                version=7;
            }
        }else if(indexSubPartArray.length>1){
//          filebeat_202103
            //202103
            dateNum=Integer.parseInt(indexSubPartArray[indexSubPartArray.length-1]);
            version=7;
        }else{
            //filebeat
            version=7;
        }
    }

    public String getDestIndex(String remoteClusterName){
        if(version==6){
            return remoteClusterName+":"+ orginalIndexName;
        }
        return orginalIndexName;
    }
}
