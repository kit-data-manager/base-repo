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
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Schema(description = "An agent related to the creation or modification of a resource, e.g. the creator or a contributor.")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Agent{

  @Id
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "Family name of the user.", example = "Doe", required = false)
  private String familyName;
  @Schema(description = "Given name of the user.", example = "John", required = false)
  private String givenName;
  @Schema(description = "Affiliation of the user, e.g. home institution.", example = "Karlsruhe Institute of Techology", required = false)
  @ElementCollection
  private Set<String> affiliations = new HashSet<>();

  public static Agent factoryAgent(String givenName, String familyName, String[] affiliations){
    Agent result = new Agent();
    result.familyName = familyName;
    result.givenName = givenName;
    result.affiliations.addAll(Arrays.asList(affiliations));
    return result;
  }

  public static Agent factoryAgent(String givenName, String familyName){
    Agent result = new Agent();
    result.familyName = familyName;
    result.givenName = givenName;
    return result;
  }

}
