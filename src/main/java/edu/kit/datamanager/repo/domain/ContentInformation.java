/*
 * Copyright 2017 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import edu.kit.datamanager.repo.util.PathUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 *
 * @author jejkal
 */
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class ContentInformation implements Serializable{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @ManyToOne
  @SecureUpdate({"FORBIDDEN"})
  private DataResource parentResource;
  @SecureUpdate({"ROLE_ADMINISTRATOR"})
  private String relativePath;
  @SecureUpdate({"FORBIDDEN"})
  private int depth;
  @SecureUpdate({"ROLE_ADMINISTRATOR"})
  private String contentUri;
  @SecureUpdate({"ROLE_ADMINISTRATOR", "PERMISSION_ADMINISTRATE"})
  private String mediaType;
  @SecureUpdate({"ROLE_ADMINISTRATOR", "PERMISSION_ADMINISTRATE"})
  private String hash;
  @SecureUpdate({"ROLE_ADMINISTRATOR", "PERMISSION_ADMINISTRATE"})
  private long size;
  @SecureUpdate({"ROLE_ADMINISTRATOR", "PERMISSION_WRITE"})
  @ElementCollection
  private Map<String, String> metadata = new HashMap<>();
  @SecureUpdate({"ROLE_ADMINISTRATOR", "PERMISSION_WRITE"})
  @ElementCollection
  private Set<String> tags = new HashSet<>();

  public static ContentInformation createContentInformation(@NonNull Long parentId, @NonNull String relativePath, String... tags){
    ContentInformation result = new ContentInformation();
    result.setRelativePath(relativePath);

    if(tags != null && (tags.length >= 1 && tags[0] != null)){
      result.getTags().addAll(Arrays.asList(tags));
    }

    DataResource res = new DataResource();
    res.setId(parentId);
    result.setParentResource(res);
    return result;
  }

  public static ContentInformation createContentInformation(String relativePath){
    ContentInformation info = new ContentInformation();
    info.setRelativePath(relativePath);
    return info;
  }

  public String getFilename(){
    if(relativePath == null){
      return "unknown.bin";
    }
    return relativePath.substring(relativePath.lastIndexOf("/") + 1);
  }

  @JsonIgnore
  public void setMediaTypeAsObject(MediaType mediaType){
    if(mediaType == null){
      throw new IllegalArgumentException("Argument must not be null.");
    }
    this.mediaType = mediaType.toString();
  }

  @JsonIgnore
  public MediaType getMediaTypeAsObject(){
    try{
      return MediaType.parseMediaType(mediaType);
    } catch(InvalidMediaTypeException ex){
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  public void setRelativePath(String path){
    if(path == null){
      throw new IllegalArgumentException("Argument must not be null.");
    }

    //remove multiple slashes
    relativePath = PathUtils.normalizePath(path);
    depth = PathUtils.getDepth(relativePath);
  }
}
