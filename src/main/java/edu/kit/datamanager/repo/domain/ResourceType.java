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
@Data
@Schema(description = "The type of a resource.")
public class ResourceType{

  public enum TYPE_GENERAL implements BaseEnum{

    AUDIOVISUAL("Audiovisual"),
    COLLECTION("Collection"),
    DATASET("Dataset"),
    EVENT("Event"),
    IMAGE("Image"),
    INTERACTIVE_RESOURCE("InteractiveResource"),
    MODEL("Model"),
    PHYSICAL_OBJECT("PhysicalObject"),
    SERVICE("Service"),
    SOFTWARE("Software"),
    SOUND("Sound"),
    TEXT("Text"),
    WORKFLOW("Workflow"),
    OTHER("Other");

    private final String value;

    private TYPE_GENERAL(String value){
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
  @Schema(description = "Measurement Data", required = true)
  private String value;
  //vocab, e.g. Dataset, Image....
  @Schema(example = "DATASET", required = true)
  @Enumerated(EnumType.STRING)
  private TYPE_GENERAL typeGeneral;

  public static ResourceType createResourceType(String value){
    ResourceType type = new ResourceType();
    type.value = value;
    type.typeGeneral = TYPE_GENERAL.DATASET;
    return type;
  }

  public static ResourceType createResourceType(String value, TYPE_GENERAL typeGeneral){
    ResourceType type = new ResourceType();
    type.value = value;
    type.typeGeneral = typeGeneral;
    return type;
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
    final ResourceType other = (ResourceType) obj;
    if(!Objects.equals(this.value, other.value)){
      return false;
    }
    if(!Objects.equals(this.id, other.id)){
      return false;
    }
    return EnumUtils.equals(this.typeGeneral, other.typeGeneral);
  }

  @Override
  public int hashCode(){
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.id);
    hash = 89 * hash + Objects.hashCode(this.value);
    hash = 89 * hash + EnumUtils.hashCode(this.typeGeneral);
    return hash;
  }

}
