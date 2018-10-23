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
package edu.kit.datamanager.repo.service;

import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.service.IGenericService;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author jejkal
 */
public interface IContentInformationService extends IGenericService<ContentInformation>, HealthIndicator{

  /**
   * Create a new content information resource using the provided template. The
   * content information will be associated with the provided data resource and
   * accessible at the provided path. The bitstream itself can either be
   * provided by the user can be read via the provided input stream, or it can
   * be referenced and is provided as contentUri attribute inside the
   * contentInformation argument. Finally, there is a 'force' argument which can
   * be set 'true' if the user wants to replace the content currently accessible
   * at the provided path.
   *
   * Depending on the combination of arguments, the attributes of the content
   * information template might be used or determined. E.g. if the user provides
   * a bitstream, attributes like checksum and size should be created based on
   * the received information. On the other hand, if the user provides the
   * bitstream as content URI, it'll probably make no sense to read the entire
   * bitstream just to obtain/validate size and checksum.
   *
   * @param contentInformation The content information template.
   * @param resource The parent resource.
   * @param path The content path.
   * @param fileStream The file stream to read user-provided content from.
   * @param force If true, existing content should be replaced.
   *
   * @return The created content information resource.
   */
  ContentInformation create(ContentInformation contentInformation, DataResource resource, String path, InputStream fileStream, boolean force);

//
//  Optional<ContentInformation> findByParentResourceIdEqualsAndRelativePathEquals(Long id, String relativePath);
//
//
//  Page<ContentInformation> findByParentResourceIdEqualsAndRelativePathLikeAndHasTag(Long id, String relativePath, String tag, Pageable pgbl);
  /**
   * Find all content information resources belonging to the provided parent
   * resource id with the provided relative path and tag. The relative path
   * provided to this method may contain wildcard characters, which depend on
   * the data backend implementation, e.g. &percnt; for SQL-based databases.
   * Furthermore, a tag can be provided. Tags are plain strings which can be
   * linked to content information resources. If the relative path denotes a
   * collection of resources, all child resources having the provided tag must
   * be returned. This allows additional structuring or categorization of
   * content information. In addition, wildcard characters may also be usable in
   * tags to achieve an even higher flexibility.
   *
   * @param id The numeric identifier of the parent resource.
   * @param relativePath The relative path information stored in the content
   * information resource.
   * @param tag The tag which has to be associated to each returned content
   * information.
   * @param pgbl The pageable object containing pagination information.
   *
   * @return A page of matching content information resources.
   */
  // Page<ContentInformation> getContentInformation(Long id, String relativePath, String tag, Pageable pgbl);
  /**
   * Find a single content information resource by its parent resource id and
   * the relative path. The provided relative path is expected to refer to a
   * specific content information resource. Thus, the combination of data
   * resource and relative path should be unique and a single content
   * information resource is returned if one exists for the provided data
   * resource and relative path. If one wants to list multiple content
   * information resources matching a certain path, {@link #getContentInformation(java.lang.Long, java.lang.String, java.lang.String, org.springframework.data.domain.Pageable)
   * } should be used.
   *
   * @param id The numeric identifier of the parent resource.
   * @param relativePath The relative path information stored in the content
   * information resource.
   *
   * @return The ContentInformation resource.
   */
  ContentInformation getContentInformation(Long id, String relativePath);
}
