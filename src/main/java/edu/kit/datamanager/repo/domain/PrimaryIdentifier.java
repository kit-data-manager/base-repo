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
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author jejkal
 */
@Entity
@Data
public class PrimaryIdentifier{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(example = "10.1234/foo", required = true)
  private String value;

  private String identifierType = "DOI";

  public static PrimaryIdentifier factoryPrimaryIdentifier(){
    return factoryPrimaryIdentifier(UnknownInformationConstants.TO_BE_ASSIGNED_OR_ANNOUNCED_LATER.getValue());
  }

  public static PrimaryIdentifier factoryPrimaryIdentifier(@NonNull String doiOrTbaConstant){
    PrimaryIdentifier result = new PrimaryIdentifier();
    result.setValue(doiOrTbaConstant);
    return result;
  }

  public static PrimaryIdentifier factoryPrimaryIdentifier(@NonNull UnknownInformationConstants unknownInformationConstant){
    return factoryPrimaryIdentifier(unknownInformationConstant.getValue());
  }

  /**
   * Check if the value of this identifier is any of the available
   * UnknownInformationConstants. If this is the case, FALSE is returned.
   * Otherwise, TRUE is returned.
   *
   * @return TRUE if the value is none of the values defined as
   * UnknownInformationConstants, FALSE if one value matches.
   */
  public boolean hasDoi(){
    for(UnknownInformationConstants constant : UnknownInformationConstants.values()){
      if(constant.getValue().equals(getValue())){
        return false;
      }
    }
    return true;
  }
}
