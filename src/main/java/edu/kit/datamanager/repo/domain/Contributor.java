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
import javax.persistence.OneToOne;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Schema(description = "A contributor to a resource.")
@Data
public class Contributor{

  public enum TYPE implements BaseEnum{
    CONTACT_PERSON("ContactPerson"),
    DATA_COLLECTOR("DataCollector"),
    DATA_CURATOR("DataCurator"),
    DATA_MANAGER("DataManager"),
    DISTRIBUTOR("Distributor"),
    EDITOR("Editor"),
    HOSTING_INSTITUTION("HostingInstitution"),
    OTHER("Other"),
    PRODUCER("Producer"),
    PROJECT_LEADER("ProjectLeader"),
    PROJECT_MANAGER("ProjectManager"),
    PROJECT_MEMBER("ProjectMember"),
    REGISTRATION_AGENCY("RegistrationAgency"),
    REGISTRATION_AUTHORITY("RegistrationAuthority"),
    RELATED_PERSON("RelatedPerson"),
    RESEARCH_GROUP("ResearchGroup"),
    RIGHTS_HOLDER("RightsHolder"),
    RESEARCHER("Researcher"),
    SPONSOR("Sponsor"),
    SUPERVISOR("Supervisor"),
    WORK_PACKAGE_LEADER("WorkPackageLeader");

    private final String value;

    TYPE(String value){
      this.value = value;
    }

    @Override
    public String getValue(){
      return value;
    }
  }

  @Id
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "Contributing user.", implementation = edu.kit.datamanager.repo.domain.Agent.class, required = true)
  @OneToOne(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private Agent user;

  //vocab, e.g. Producer, Editor...
  @Schema(description = "Controlled vocabulary value describing the contribution type, e.g. Producer.", required = true)
  @Enumerated(EnumType.STRING)
  private TYPE contributionType;

  public static Contributor factoryContributor(Agent agent, TYPE type){
    Contributor result = new Contributor();
    result.user = agent;
    result.contributionType = type;
    return result;
  }

  @Override
  public int hashCode(){
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.id);
    hash = 89 * hash + Objects.hashCode(this.user);
    hash = 89 * hash + EnumUtils.hashCode(this.contributionType);
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
    final Contributor other = (Contributor) obj;
    if(!Objects.equals(this.id, other.id)){
      return false;
    }
    if(!Objects.equals(this.user, other.user)){
      return false;
    }

    return EnumUtils.equals(this.contributionType, other.contributionType);
  }

}
