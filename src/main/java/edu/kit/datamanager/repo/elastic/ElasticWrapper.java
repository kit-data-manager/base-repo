/*
 * Copyright 2023 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.repo.elastic;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date.DATE_TYPE;
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
 * Wrapper for indexing repo resources in Elastic.
 *
 * @author jejkal
 */
@Document(indexName = "baserepo")
public class ElasticWrapper {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String pid;

    @Field(type = FieldType.Object, name = "metadata")
    private DataResource metadata;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ContentInformation> content = new ArrayList<>();

    @Field(type = FieldType.Text)
    private List<String> read = new ArrayList<>();

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
        resource.getAcls().forEach(entry -> {
            String sid = entry.getSid();
            if (entry.getPermission().atLeast(PERMISSION.READ)) {
                read.add(sid);
            }
        });

        resource.getDates().stream().filter(d -> (DATE_TYPE.CREATED.equals(d.getType()))).forEachOrdered(d -> {
            created = Date.from(d.getValue());
        });

        lastUpdate = Date.from(Instant.now());
    }

}
