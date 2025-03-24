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
package edu.kit.datamanager.repo.util;

import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.Contributor;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Date;
import edu.kit.datamanager.repo.domain.Description;
import edu.kit.datamanager.repo.domain.Scheme;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.RoCrate.RoCrateBuilder;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import edu.kit.datamanager.util.AuthenticationHelper;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jejkal
 */
public class ROCrateUtils {

    public static RoCrate fromDataResource(DataResource resource, List<ContentInformation> contentInformation) {
        ContextualEntity license = getCrateLicenseForResource(resource);

        RoCrate.RoCrateBuilder roCrateBuilder = new RoCrate.RoCrateBuilder(getCrateNameForResource(resource),
                getCrateDescriptionForResource(resource),
                getCrateCreationDateForResource(resource),
                license);

        if(isOpen(resource)){
            //add content as reference
            
        }else{
            //add content as value
        }
        //check if anonymousAccess possible (if so, allow crate using references, otherwise integrate data only)
        //if integrate data:
        //create temp folder
        //obtain resoure metadata
        //obtain files
        //use RO-Crate Builder to merge all
        //Zip and return
        //else
        //add resource metadata as reference (possible?)
        //iterate though content information and add metadata + content url
        //use RO-Crate Builder to merge all
        //Zip and return/return only metadata via content negotiation?
        return roCrateBuilder.build();

    }

    private static String getCrateNameForResource(DataResource resource) {
        String crateName = resource.getId() + " v " + resource.getVersion();
        if (!resource.getTitles().isEmpty()) {
            crateName = resource.getTitles().iterator().next().getValue();
        }
        return crateName;
    }

    private static String getCrateDescriptionForResource(DataResource resource) {
        String description = "RO-Crate for resource " + resource.getId() + ", version " + resource.getVersion();
        if (!resource.getDescriptions().isEmpty()) {
            Description descriptionEntry = resource.getDescriptions().iterator().next();
            description = descriptionEntry.getDescription();
        }
        return description;
    }

    private static ContextualEntity getCrateLicenseForResource(DataResource resource) {
        ContextualEntity license = null;
        if (!resource.getRights().isEmpty()) {
            Scheme rightEntry = resource.getRights().iterator().next();
            license = new ContextualEntity.ContextualEntityBuilder().
                    addType("CreativeWork").
                    setId(rightEntry.getSchemeUri()).
                    addProperty("name", rightEntry.getSchemeId())
                    .build();
        }
        return license;
    }

    private static String getCrateCreationDateForResource(DataResource resource) {
        Set<Date> dates = resource.getDates();
        String crateCreationDate = OffsetDateTime.now().toString();
        if (!dates.isEmpty()) {
            for (Date current : dates) {
                if (Date.DATE_TYPE.CREATED.equals(current.getType())) {
                    crateCreationDate = OffsetDateTime.ofInstant(current.getValue(), ZoneId.systemDefault()).toString();
                    break;
                }
            }
        }
        return crateCreationDate;
    }

    private static void processPersons(DataResource resource, RoCrateBuilder builder) {
        if (!resource.getCreators().isEmpty()) {
            for (Agent creator : resource.getCreators()) {
                String name = (creator.getGivenName() != null) ? creator.getGivenName() + " " : "";
                name += (creator.getFamilyName() != null) ? creator.getFamilyName() : "";
                PersonEntity.PersonEntityBuilder person = new PersonEntity.PersonEntityBuilder().setId("#" + creator.getFamilyName()).addProperty("name", name);

                if (creator.getAffiliations() != null && !creator.getAffiliations().isEmpty()) {
                    for (String affiliation : creator.getAffiliations()) {
                        person.addProperty("affiliation", affiliation);
                    }
                }
                builder.addContextualEntity(person.build());

            }
        }

        if (!resource.getContributors().isEmpty()) {
            for (Contributor contributor : resource.getContributors()) {
                Agent user = contributor.getUser();
                String name = (user.getGivenName() != null) ? user.getGivenName() + " " : "";
                name += (user.getFamilyName() != null) ? user.getFamilyName() : "";
                PersonEntity.PersonEntityBuilder person = new PersonEntity.PersonEntityBuilder().setId("#" + user.getFamilyName()).addProperty("name", name);

                if (user.getAffiliations() != null && !user.getAffiliations().isEmpty()) {
                    for (String affiliation : user.getAffiliations()) {
                        person.addProperty("affiliation", affiliation);
                    }
                }

                if (contributor.getContributionType() != null) {
                    person.addProperty("jobTitle", contributor.getContributionType().toString());
                }

                builder.addContextualEntity(person.build());
            }
        }

    }

    private static boolean isOpen(DataResource resource) {
        for (AclEntry entry : resource.getAcls()) {
            if (AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL.equals(entry.getSid()) && entry.getPermission().atLeast(PERMISSION.READ)) {
                return true;
            }
        }
        return false;
    }

}
