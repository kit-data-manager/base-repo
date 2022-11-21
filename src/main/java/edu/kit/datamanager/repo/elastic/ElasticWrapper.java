/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.kit.datamanager.repo.elastic;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 *
 * @author Torridity
 */
@Document(indexName = "baserepo")
public class ElasticWrapper {

    @Id
    private String id;

    private String pid;

    @Field(type = FieldType.Object, name = "metadata")
    private DataResource metadata;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ContentInformation> content = new ArrayList<>();

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<String> readSids = new ArrayList<>();

    private Map<String, String> links = new HashMap<>();

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date created;

    @Field(type = FieldType.Date, format = DateFormat.basic_date_time)
    private Date lastUpdate;

    public ElasticWrapper(DataResource resource) {
        this(resource, Collections.emptyList());
    }

    public ElasticWrapper(DataResource resource, List<ContentInformation> content) {
        id = resource.getId();
        pid = (resource.getIdentifier() != null) ? resource.getIdentifier().getValue() : null;
        metadata = resource;
        this.content = content;
        for (AclEntry entry : resource.getAcls()) {
            String sid = entry.getSid();
            if ("anonyous".equals(sid)) {
                //public access
                readSids.clear();
                break;
            }
            if (entry.getPermission().atLeast(PERMISSION.READ)) {
                readSids.add(sid);
            }
        }

        resource.getDates().stream().filter(d -> (edu.kit.datamanager.entities.repo.Date.DATE_TYPE.CREATED.equals(d.getType()))).forEachOrdered(d -> {
            created = Date.from(d.getValue());
        });

        lastUpdate = Date.from(Instant.now());
    }

}
