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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "A resource's funding information.")
@Data
public class FundingReference{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @ApiModelProperty(dataType = "String", required = true)
  private String funderName;
  //use identifier?
  @ApiModelProperty(required = false)
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private FunderIdentifier funderIdentifier;
  @ApiModelProperty(required = false)
  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  private Scheme awardNumber;
  @ApiModelProperty(dataType = "String", required = false)
  private String awardUri;
  @ApiModelProperty(dataType = "String", required = false)
  private String awardTitle;

  public static FundingReference factoryFundingReference(String funderName, FunderIdentifier funderIdentifier, Scheme awardNumber, String awardUri, String awardTitle){
    FundingReference result = new FundingReference();
    result.funderName = funderName;
    result.funderIdentifier = funderIdentifier;
    result.awardNumber = awardNumber;
    result.awardUri = awardUri;
    result.awardTitle = awardTitle;
    return result;
  }

}
