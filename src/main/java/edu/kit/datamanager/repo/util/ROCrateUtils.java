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

import edu.kit.datamanager.repo.domain.Agent;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.Contributor;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.Scheme;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.ro_crate.RoCrate;
import edu.kit.datamanager.ro_crate.RoCrate.RoCrateBuilder;
import edu.kit.datamanager.ro_crate.entities.contextual.ContextualEntity;
import edu.kit.datamanager.ro_crate.entities.contextual.PersonEntity;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jejkal
 */
public class ROCrateUtils {

    public static RoCrate fromDataResource(DataResource resource, List<ContentInformation> contentInformation) {
        String identifier = resource.getId();
        String version = resource.getVersion();
        Set<Title> titles = resource.getTitles();
        String crateName = identifier + " v " + version;
        if (!titles.isEmpty()) {
            crateName = titles.iterator().next().getValue();
        }

        RoCrate.RoCrateBuilder roCrateBuilder = new RoCrate.RoCrateBuilder(crateName, "RO-Crate for resource " + identifier + ", version " + version);

        processLicense(resource, roCrateBuilder);

        return roCrateBuilder.build();

    }

    public static void processLicense(DataResource resource, RoCrateBuilder builder) {
        if (!resource.getRights().isEmpty()) {
            for (Scheme rightEntry : resource.getRights()) {
                ContextualEntity license = new ContextualEntity.ContextualEntityBuilder().
                        addType("CreativeWirk").
                        setId(rightEntry.getSchemeUri()).
                        addProperty("name", rightEntry.getSchemeId())
                        .build();
                builder.addContextualEntity(license);
            }
        }
    }

    public static void processPersons(DataResource resource, RoCrateBuilder builder) {
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

}
