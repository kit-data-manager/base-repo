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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import edu.kit.datamanager.entities.BaseEnum;
import edu.kit.datamanager.util.EnumUtils;
import edu.kit.datamanager.util.json.CustomInstantDeserializer;
import edu.kit.datamanager.util.json.CustomInstantSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@Schema(description = "A data entry of a resource.")
@Data
public class Date{

  //Date types
  public enum DATE_TYPE implements BaseEnum{
    ACCEPTED("Accepted"),
    AVAILABLE("Available"),
    COLLECTED("Collected"),
    COPYRIGHTED("Copyrighted"),
    CREATED("Created"),
    ISSUED("Issued"),
    SUBMITTED("Submitted"),
    UPDATED("Updated"),
    VALID("Valid"),
    REVOKED("Revoked");

    private final String value;

    DATE_TYPE(String value){
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
  //ISO format
  @Schema(description = "The actual date of the entry.", example = "2017-05-10T10:41:00Z", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
  @JsonDeserialize(using = CustomInstantDeserializer.class)
  @JsonSerialize(using = CustomInstantSerializer.class)
  Instant value;
  //vocab, e.g. Created, Issued...
  @Schema(description = "Controlled vocabulary value describing the date type.", required = true)
  @Enumerated(EnumType.STRING)
  DATE_TYPE type;

  public static Date factoryDate(Instant value, DATE_TYPE type){
    Date result = new Date();
    result.setValue(value);
    result.type = type;
    return result;
  }

  public void setValue(Instant value){
    this.value = Objects.requireNonNull(value).truncatedTo(ChronoUnit.SECONDS);
  }

  public Instant getValue(){
    return value;
  }

  @Override
  public int hashCode(){
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.id);
    hash = 89 * hash + Objects.hashCode(this.value);
    hash = 89 * hash + EnumUtils.hashCode(this.type);
    return hash;
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
    final Date other = (Date) obj;
    if(!Objects.equals(this.id, other.id)){
      return false;
    }

    if(!Objects.equals(this.value, other.value)){
      return false;
    }
    return EnumUtils.equals(this.type, other.type);
  }

}
