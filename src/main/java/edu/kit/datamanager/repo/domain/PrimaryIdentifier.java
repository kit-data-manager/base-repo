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

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.util.EnumUtils;
import java.util.Objects;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @author jejkal
 */
@Entity(name = "PrimaryIdentifier")
@DiscriminatorValue("PrimaryIdentifier")
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimaryIdentifier extends Identifier{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;

  public static PrimaryIdentifier factoryPrimaryIdentifier(){
    return factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER.getValue());
  }

  public static PrimaryIdentifier factoryPrimaryIdentifier(String doiOrTbaConstant){
    PrimaryIdentifier result = new PrimaryIdentifier();
    result.setIdentifierType(IDENTIFIER_TYPE.DOI);
    result.setValue(doiOrTbaConstant);
    return result;
  }

  public static PrimaryIdentifier factoryPrimaryIdentifier(UnknownInformationConstants unknownInformationConstant){
    return factoryPrimaryIdentifier(unknownInformationConstant.getValue());
  }

  @Override
  public void setIdentifierType(IDENTIFIER_TYPE identifierType){
    //does nothing as primary identifier is always doi
  }

  @Override
  public IDENTIFIER_TYPE getIdentifierType(){
    return IDENTIFIER_TYPE.DOI;
  }

  public boolean hasDoi(){
    return !UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER.getValue().equals(getValue());
  }
}
