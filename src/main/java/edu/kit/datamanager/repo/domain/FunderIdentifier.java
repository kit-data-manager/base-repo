/*
 * Copyright 2018 Karlsruhe Institute of Technology.
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

import edu.kit.datamanager.entities.BaseEnum;
import edu.kit.datamanager.entities.Identifier;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author jejkal
 */
@Entity(name = "FunderIdentifier")
@DiscriminatorValue("FunderIdentifier")
@Data
@EqualsAndHashCode(callSuper = false)
public class FunderIdentifier extends Identifier{

  public enum FUNDER_TYPE implements BaseEnum{
    ISNI("ISNI"),
    GRID("GRID"),
    CROSSREF_FUNDER_ID("Crossref Funder ID"),
    OTHER("Other");

    private final String value;

    private FUNDER_TYPE(String value){
      this.value = value;
    }

    @Override
    public String getValue(){
      return value;
    }
  }
  @Enumerated(EnumType.STRING)
  private FUNDER_TYPE type;

  public static FunderIdentifier factoryIdentifier(String value, FUNDER_TYPE type){
    FunderIdentifier result = new FunderIdentifier(type);
    result.setValue(value);
    return result;
  }

  FunderIdentifier(){
    super();
  }

  public FunderIdentifier(FUNDER_TYPE type){
    this.type = type;
  }
//
//  @Override
//  public boolean equals(Object obj){
//    if(this == obj){
//      return true;
//    }
//    if(obj == null){
//      return false;
//    }
//    if(getClass() != obj.getClass()){
//      return false;
//    }
//    final FunderIdentifier other = (FunderIdentifier) obj;
//    if(!Objects.equals(this.getId(), other.getId())){
//      return false;
//    }
//
//    if(!Objects.equals(this.getValue(), other.getValue())){
//      return false;
//    }
//    return EnumUtils.equals(this.type, other.type);
//  }
//
//  @Override
//  public int hashCode(){
//    int hash = 5;
//    hash = 67 * hash + Objects.hashCode(this.getId());
//    hash = 67 * hash + Objects.hashCode(this.getValue());
//    hash = 67 * hash + EnumUtils.hashCode(this.type);
//    return hash;
//  }
}
