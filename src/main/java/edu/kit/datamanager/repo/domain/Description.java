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

import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import edu.kit.datamanager.entities.BaseEnum;
import edu.kit.datamanager.util.EnumUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Schema(description = "A description entry of a resource.")
@Data
public class Description{

  public enum TYPE implements BaseEnum{
    ABSTRACT("Abstract"),
    METHODS("Methods"),
    SERIES_INFORMATION("SeriesInformation"),
    TABLE_OF_CONTENTS("TableOfContents"),
    TECHNICAL_INFO("TechnicalInfo"),
    OTHER("Other");

    private final String value;

    private TYPE(String value){
      this.value = value;
    }

    @Override
    public String getValue(){
      return value;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "The actual description as full text.", required = true)
  private String description;
  //vocab, e.g. Abstract
  @Schema(description = "Controlled vocabulary value describing the description type.", required = true)
  @Enumerated(EnumType.STRING)
  private TYPE type;
  @Schema(description = "Description language.", required = false)
  private String lang;

  public static Description factoryDescription(String description, TYPE type, String lang){
    Description result = new Description();
    result.description = description;
    result.type = type;
    result.lang = lang;
    return result;
  }

  public static Description factoryDescription(String description, TYPE type){
    Description result = new Description();
    result.description = description;
    result.type = type;
    return result;
  }

  @Override
  public boolean equals(Object obj){
    if(this == obj){
      return true;
    }
    if(obj == null){
      return false;
    }
    if(getClass() != obj.getClass()){
      return false;
    }
    final Description other = (Description) obj;
    if(!Objects.equals(this.description, other.description)){
      return false;
    }
    if(!Objects.equals(this.lang, other.lang)){
      return false;
    }
    if(!Objects.equals(this.id, other.id)){
      return false;
    }
    return EnumUtils.equals(this.type, other.type);
  }

  @Override
  public int hashCode(){
    int hash = 3;
    hash = 97 * hash + Objects.hashCode(this.id);
    hash = 97 * hash + Objects.hashCode(this.description);
    hash = 97 * hash + EnumUtils.hashCode(this.type);
    hash = 97 * hash + Objects.hashCode(this.lang);
    return hash;
  }
}
