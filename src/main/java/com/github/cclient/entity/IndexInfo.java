package com.github.cclient.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity(name = "index_info")
//@ApiModel("$column.comments")
//@AllArgsConstructor
//@NoArgsConstructor
public class IndexInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "index")
    private String index;

    @Column(name = "dest_index")
    private String destIndex;

    @Column(name = "remote_cluster_name")
    private String remoteClusterName;

    @Column(name = "version")
    private Integer version;

    @Column(name = "es_type")
    private String esType;
}
