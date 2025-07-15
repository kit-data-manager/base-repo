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
import edu.kit.datamanager.ro_crate.entities.data.DataEntity;
import edu.kit.datamanager.ro_crate.entities.data.FileEntity.FileEntityBuilder;
import edu.kit.datamanager.util.AuthenticationHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/**
 * @author jejkal
 */
public class ROCrateUtils {

    public static RoCrate fromDataResource(DataResource resource, List<ContentInformation> contentInformation, String baseUrl) {
        ContextualEntity license = getCrateLicenseForResource(resource);

        RoCrate.RoCrateBuilder roCrateBuilder = new RoCrate.RoCrateBuilder(getCrateNameForResource(resource),
                getCrateDescriptionForResource(resource),
                getCrateCreationDateForResource(resource),
                license);

        processPersons(resource, roCrateBuilder);
        boolean useReference = isOpen(resource);
        for (ContentInformation content : contentInformation) {
            roCrateBuilder.addDataEntity(dataEntityFromContentInformation(content, baseUrl, useReference));
        }
        return roCrateBuilder.build();
    }

    /**
     * Get the name (title) for the provided resource.
     *
     * @param resource The resource.
     * @return String The name value.
     */
    private static String getCrateNameForResource(DataResource resource) {
        String crateName = resource.getId() + " v " + resource.getVersion();
        if (!resource.getTitles().isEmpty()) {
            crateName = resource.getTitles().iterator().next().getValue();
        }
        return crateName;
    }

    /**
     * Get the description for the provided resource.
     *
     * @param resource The resource.
     * @return String The description value.
     */
    private static String getCrateDescriptionForResource(DataResource resource) {
        String description = "RO-Crate for resource " + resource.getId() + ", version " + resource.getVersion();
        if (!resource.getDescriptions().isEmpty()) {
            Description descriptionEntry = resource.getDescriptions().iterator().next();
            description = descriptionEntry.getDescription();
        }
        return description;
    }

    /**
     * Get the license contextual entity for the provided resource.
     *
     * @param resource The resource.
     * @return ContextualEntity The license entity.
     */
    private static ContextualEntity getCrateLicenseForResource(DataResource resource) {
        ContextualEntity license = null;
        if (!resource.getRights().isEmpty()) {
            Scheme rightEntry = resource.getRights().iterator().next();
            license = new ContextualEntity.ContextualEntityBuilder().
                    addType("CreativeWork").
                    setId(rightEntry.getSchemeUri()).
                    addProperty("name", rightEntry.getSchemeId())
                    .build();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "RO Crate export not acceptable for resources without license.");
        }
        return license;
    }

    /**
     * Obtain the creation data from the provided resource.
     *
     * @param resource The resource.
     * @return String The creation date in ISO format.
     */
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

    /**
     * Process persons from resource, i.e., creators and contributor, and add
     * them to the crate builder.
     *
     * @param resource The resource to process.
     * @param builder  The RO-Crate builder to use.
     */
    private static void processPersons(DataResource resource, RoCrateBuilder builder) {
        if (!resource.getCreators().isEmpty()) {
            for (Agent creator : resource.getCreators()) {
                //only add creator if family name is set, i.e., not for SELF
                if (creator.getFamilyName() != null) {
                    PersonEntity.PersonEntityBuilder person = agentToPersonEntity(creator);

                    if (creator.getAffiliations() != null && !creator.getAffiliations().isEmpty()) {
                        for (String affiliation : creator.getAffiliations()) {
                            person.addProperty("affiliation", affiliation);
                        }
                    }
                    builder.addContextualEntity(person.build());
                }
            }
        }

        if (!resource.getContributors().isEmpty()) {
            for (Contributor contributor : resource.getContributors()) {
                Agent user = contributor.getUser();
                if (user.getFamilyName() != null) {
                    PersonEntity.PersonEntityBuilder person = agentToPersonEntity(user);

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

    private static PersonEntity.PersonEntityBuilder agentToPersonEntity(Agent agent) {
        String name = (agent.getGivenName() != null) ? agent.getGivenName() + " " : "";
        name += (agent.getFamilyName() != null) ? agent.getFamilyName() : "";
        return new PersonEntity.PersonEntityBuilder().setId("#" + agent.getFamilyName()).addProperty("name", name);
    }

    /**
     * Check if annonymous access is allowed or not. If resource is open, data
     * entities can be added as reference, if resource is not open, data
     * entities must be added by value.
     *
     * @param resource Resource to test.
     * @return boolean TRUE = open, FALSE = closed
     */
    private static boolean isOpen(DataResource resource) {
        for (AclEntry entry : resource.getAcls()) {
            if (AuthenticationHelper.ANONYMOUS_USER_PRINCIPAL.equals(entry.getSid()) && entry.getPermission().atLeast(PERMISSION.READ)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a data entity from the provided content information. There are two possible results: if byReference is true, a data entity pointing to
     * the API endpoint using baseUrl is set as @id for direct access. This is only possible for openAccess resources. If byReference is false, the
     * file will be included by value, i.e., it will be part of the crate.
     * In any case, additional metadata will be extracted if available, e.g., mediaType, contentSize, version, and keywords (comma separated tags).
     *
     * @param contentInformation The content information to extract metadata from.
     * @param baseUrl            The baseUrl of the baseRepo instance used for building the direct access URL to the content.
     * @param byReference        If true, the content will be added as URL for direct access, if false, the file is added by value.
     * @return DataEntity A new data entity.
     */
    private static DataEntity dataEntityFromContentInformation(ContentInformation contentInformation, String baseUrl, boolean byReference) {
        FileEntityBuilder file = new FileEntityBuilder();
        file.setId(contentInformation.getRelativePath());
        if (byReference) {
            file = file.setLocation(URI.create(baseUrl + "/data/" + contentInformation.getRelativePath()));
        } else {
            file = file.setLocation(Paths.get(URI.create(contentInformation.getContentUri())));
        }

        file = file.addProperty("name", contentInformation.getRelativePath())
                .setEncodingFormat(contentInformation.getMediaType())
                .addProperty("contentSize", Long.toString(contentInformation.getSize()))
                .addProperty("version", contentInformation.getVersion());

        if (contentInformation.getTags() != null && !contentInformation.getTags().isEmpty()) {
            file = file.addProperty("keywords", String.join(",", contentInformation.getTags()));
        }

        return file.build();
    }

}
