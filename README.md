## proxy multi elasticsearch cluster's _search and _bulk api

https://www.elastic.co/guide/en/elasticsearch/reference/7.10/modules-remote-clusters.html

use elasticsearch remote cluster to proxy elasticsearch cluster's _search and _bulk api

tested work on es6.8.14 and es7.10.2

to update es cluster by rolling replace index,assign new index on es7 and close old index on es6 

### prepare env / 准备测试环境

#### 1 start es6.8.14 启动es6.8.14

`docker run -d --name es6 -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.8.14`

<details>
  <summary>start es6.8.14</summary>
  <pre><code> 
```bash
curl 127.0.0.1:9200
{
  "name" : "QbmirZL",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "OCTVBSSDRoy2Aa9_DxttOQ",
  "version" : {
    "number" : "6.8.14",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "dab5822",
    "build_date" : "2021-02-02T19:58:04.182039Z",
    "build_snapshot" : false,
    "lucene_version" : "7.7.3",
    "minimum_wire_compatibility_version" : "5.6.0",
    "minimum_index_compatibility_version" : "5.0.0"
  },
  "tagline" : "You Know, for Search"
}
```
  </code></pre>
</details>

#### 2 start es7.10.2 启动es7.10.2

`docker run -d --link es6:es6 --name es7 -p 9201:9200 -p 9301:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.10.2-amd64`


<details>
  <summary>start es7.10.2</summary>
  <pre><code> 
```bash
curl 127.0.0.1:9201
{
    "name": "7dbc155608e2",
    "cluster_name": "docker-cluster",
    "cluster_uuid": "6TrlPyMUTaeSOc3WM_WYPw",
    "version": {
        "number": "7.10.2",
        "build_flavor": "default",
        "build_type": "docker",
        "build_hash": "747e1cc71def077253878a59143c1f785afa92b9",
        "build_date": "2021-01-13T00:42:12.435326Z",
        "build_snapshot": false,
        "lucene_version": "8.7.0",
        "minimum_wire_compatibility_version": "6.8.0",
        "minimum_index_compatibility_version": "6.0.0-beta1"
    },
    "tagline": "You Know, for Search"
}
```
  </code></pre>
</details>

#### 3 register es6.8.14 as remote-cluster in es7.10.2 注册es6.8.14至es7.10.2

```bash

curl -XPUT http://127.0.0.1:9201/_cluster/settings -H 'Content-Type:application/json' -d'{
  "persistent": {
    "cluster": {
      "remote": {
        "o": {
          "skip_unavailable": false,
          "mode": "sniff",
          "proxy_address": null,
          "proxy_socket_connections": null,
          "server_name": null,
          "seeds": [
            "es6:9300"
          ],
          "node_connections": 1
        }
      }
    }
  }
}'
---
{
    "acknowledged": true,
    "persistent": {
        "cluster": {
            "remote": {
                "o": {
                    "mode": "sniff",
                    "skip_unavailable": "false",
                    "node_connections": "1",
                    "seeds": [
                        "es6:9300"
                    ]
                }
            }
        }
    },
    "transient": {}
}
---
curl http://127.0.0.1:9201/_remote/info
{
    "o": {
        "connected": true,
        "mode": "sniff",
        "seeds": [
            "es6:9300"
        ],
        "num_nodes_connected": 1,
        "max_connections_per_cluster": 1,
        "initial_connect_timeout": "30s",
        "skip_unavailable": false
    }
}
```

#### 4 start mysql

`docker run --name es-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=my-root-passwd -e MYSQL_DATABASE=es_compat -e MYSQL_USER=es_compat -e MYSQL_PASSWORD=es_compat_passwd -d mysql:5.7.33`

#### 5 create mysql table

`mysql -h 127.0.0.1 -u es_compat -P 3306 es_compat -p `

```sql
CREATE TABLE `index_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `index` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dest_index` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remote_cluster_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int(11) DEFAULT NULL,
  `es_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uni_index` (`index`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 6 build && start proxy 

`mvn clean package -Dmaven.test.skip=true`

`java -jar target/elasticsearch-multi-cluster-compat-proxy-1.0.0.jar`

---

#### delete index
`curl -XDELETE  "http://127.0.0.1:9200/filebeat_202012_log"`
`curl -XDELETE  "http://127.0.0.1:9201/filebeat_202101_log"`

`curl http://127.0.0.1:9200/_search`

```json
{
    "took": 0,
    "timed_out": false,
    "_shards": {
        "total": 0,
        "successful": 0,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": 0,
        "max_score": 0,
        "hits": []
    }
}
```

`curl http://127.0.0.1:9201/_search`

```json
{
    "took": 0,
    "timed_out": false,
    "_shards": {
        "total": 0,
        "successful": 0,
        "skipped": 0,
        "failed": 0
    },
    "hits": {
        "total": {
            "value": 0,
            "relation": "eq"
        },
        "max_score": 0,
        "hits": []
    }
}
```

####  `direct _bulk write to es6.8.14` 直接`_bulk`写入`es6.8.14`
`curl -XPOST --header "Content-Type: application/json" "http://127.0.0.1:9200/_bulk" --data-binary @data/direct2es6`

<details>
  <summary>direct _bulk write to es6.8.14</summary>
  <pre><code> 
```json
{
    "took": 1116,
    "errors": false,
    "items": [
        {
            "index": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "1",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 0,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "delete": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "1",
                "_version": 2,
                "result": "deleted",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 1,
                "_primary_term": 1,
                "status": 200
            }
        },
        {
            "create": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "2",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 0,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "update": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "2",
                "_version": 2,
                "result": "updated",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 1,
                "_primary_term": 1,
                "status": 200
            }
        }
    ]
}
```
  </code></pre>
</details>
`curl http://127.0.0.1:9200/_search`

```json
{
  "took": 48,
  "timed_out": false,
  "_shards": {
    "total": 5,
    "successful": 5,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": 1,
    "max_score": 1,
    "hits": [
      {
        "_index": "filebeat_202012_log",
        "_type": "log",
        "_id": "2",
        "_score": 1,
        "_source": {
          "field1": "direct create to es6 _id:2",
          "field2": "direct update to es6 _id:2"
        }
      }
    ]
  }
}
```


####  `direct _bulk write to es7.10.2` 直接`_bulk`写入`es7.10.2`

`curl -XPOST --header "Content-Type: application/json" "http://127.0.0.1:9201/_bulk" --data-binary @data/direct2es7`


<details>
  <summary>direct _bulk write to es7.10.2</summary>
  <pre><code> 
```
{
    "took": 905,
    "errors": false,
    "items": [
        {
            "index": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "1",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 0,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "delete": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "1",
                "_version": 2,
                "result": "deleted",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 1,
                "_primary_term": 1,
                "status": 200
            }
        },
        {
            "create": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "2",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 2,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "update": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "2",
                "_version": 2,
                "result": "updated",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 3,
                "_primary_term": 1,
                "status": 200
            }
        }
    ]
}
```
  </code></pre>
</details>

`curl http://127.0.0.1:9201/_search`

```json
{
  "took": 50,
  "timed_out": false,
  "_shards": {
    "total": 1,
    "successful": 1,
    "skipped": 0,
    "failed": 0
  },
  "hits": {
    "total": {
      "value": 1,
      "relation": "eq"
    },
    "max_score": 1,
    "hits": [
      {
        "_index": "filebeat_202101_log",
        "_type": "_doc",
        "_id": "2",
        "_score": 1,
        "_source": {
          "field1": "direct create to es7 _id:2",
          "field2": "direct update to es7 _id:2"
        }
      }
    ]
  }
}
```

#### `_search` by gateway 通过网关查询

`curl --header "Content-Type: application/json" "http://127.0.0.1:9208/filebeat_202012_log,filebeat_202101_log/_search?size=5&timeout=120s"`

```json
{
  "took": 31,
  "timed_out": false,
  "num_reduce_phases": 3,
  "_shards": {
    "total": 6,
    "successful": 6,
    "skipped": 0,
    "failed": 0
  },
  "_clusters": {
    "total": 2,
    "successful": 2,
    "skipped": 0
  },
  "hits": {
    "total": {
      "value": 2,
      "relation": "eq"
    },
    "max_score": 1,
    "hits": [
      {
        "_index": "filebeat_202101_log",
        "_type": "_doc",
        "_id": "2",
        "_score": 1,
        "_source": {
          "field1": "direct create to es7 _id:2",
          "field2": "direct update to es7 _id:2"
        }
      },
      {
        "_index": "o:filebeat_202012_log",
        "_type": "log",
        "_id": "2",
        "_score": 1,
        "_source": {
          "field1": "direct create to es6 _id:2",
          "field2": "direct update to es6 _id:2"
        }
      }
    ]
  }
}
```


#### `_bulk` by gateway 通过网关写入

`curl -XPOST --header "Content-Type: application/x-ndjson" "http://127.0.0.1:9208/_bulk" --data-binary @data/gateway2es`

<details>
  <summary>`_bulk` by gateway</summary>
  <pre><code> 
{
    "took": 66,
    "errors": false,
    "items": [
        {
            "index": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "11",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 4,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "delete": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "11",
                "_version": 2,
                "result": "deleted",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 5,
                "_primary_term": 1,
                "status": 200
            }
        },
        {
            "create": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "12",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 6,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "update": {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "12",
                "_version": 2,
                "result": "updated",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 7,
                "_primary_term": 1,
                "status": 200
            }
        },
        {
            "index": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "11",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 0,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "delete": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "11",
                "_version": 2,
                "result": "deleted",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 1,
                "_primary_term": 1,
                "status": 200
            }
        },
        {
            "create": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "12",
                "_version": 1,
                "result": "created",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 0,
                "_primary_term": 1,
                "status": 201
            }
        },
        {
            "update": {
                "_index": "filebeat_202012_log",
                "_type": "log",
                "_id": "12",
                "_version": 2,
                "result": "updated",
                "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                },
                "_seq_no": 1,
                "_primary_term": 1,
                "status": 200
            }
        }
    ]
}
  </code></pre>
</details>
#### `_search` by gateway 通过网关查询-以es7标准

`curl --header "Content-Type: application/json" "http://127.0.0.1:9208/filebeat_202012_log,filebeat_202101_log/_search?size=5&timeout=120s"`

```json

{
    "took": 1136,
    "timed_out": false,
    "num_reduce_phases": 3,
    "_shards": {
        "total": 6,
        "successful": 6,
        "skipped": 0,
        "failed": 0
    },
    "_clusters": {
        "total": 2,
        "successful": 2,
        "skipped": 0
    },
    "hits": {
        "total": {
            "value": 4,
            "relation": "eq"
        },
        "max_score": 1,
        "hits": [
            {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "field1": "direct create to es7 _id:2",
                    "field2": "direct update to es7 _id:2"
                }
            },
            {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "12",
                "_score": 1,
                "_source": {
                    "field1": "gateway create to es7 _id:12",
                    "field2": "gateway update to es7 _id:12"
                }
            },
            {
                "_index": "o:filebeat_202012_log",
                "_type": "log",
                "_id": "12",
                "_score": 1,
                "_source": {
                    "field1": "gateway create to es6 _id:12",
                    "field2": "gateway update to es6 _id:12"
                }
            },
            {
                "_index": "o:filebeat_202012_log",
                "_type": "log",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "field1": "direct create to es6 _id:2",
                    "field2": "direct update to es6 _id:2"
                }
            }
        ]
    }
}

```

#### `_search` by gateway 通过网关查询-以es6标准

`curl --header "Content-Type: application/json" "http://127.0.0.1:9208/filebeat_202012_*,filebeat_202101_*/log/_search?size=5&timeout=120s"`

```json

{
    "took": 1136,
    "timed_out": false,
    "num_reduce_phases": 3,
    "_shards": {
        "total": 6,
        "successful": 6,
        "skipped": 0,
        "failed": 0
    },
    "_clusters": {
        "total": 2,
        "successful": 2,
        "skipped": 0
    },
    "hits": {
        "total": {
            "value": 4,
            "relation": "eq"
        },
        "max_score": 1,
        "hits": [
            {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "field1": "direct create to es7 _id:2",
                    "field2": "direct update to es7 _id:2"
                }
            },
            {
                "_index": "filebeat_202101_log",
                "_type": "_doc",
                "_id": "12",
                "_score": 1,
                "_source": {
                    "field1": "gateway create to es7 _id:12",
                    "field2": "gateway update to es7 _id:12"
                }
            },
            {
                "_index": "o:filebeat_202012_log",
                "_type": "log",
                "_id": "12",
                "_score": 1,
                "_source": {
                    "field1": "gateway create to es6 _id:12",
                    "field2": "gateway update to es6 _id:12"
                }
            },
            {
                "_index": "o:filebeat_202012_log",
                "_type": "log",
                "_id": "2",
                "_score": 1,
                "_source": {
                    "field1": "direct create to es6 _id:2",
                    "field2": "direct update to es6 _id:2"
                }
            }
        ]
    }
}

```
End
