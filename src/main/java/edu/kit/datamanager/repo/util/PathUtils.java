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

import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.service.exceptions.CustomInternalServerError;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import org.apache.http.client.utils.URIBuilder;

/**
 *
 * @author jejkal
 */
public class PathUtils{

  /**
   * Obtain the absolute data uri for the provided data resource and relative
   * data path. The final URI will contain the relative data data path appended
   * to a base URI depending on the repository configuration. In addition, the
   * current timestamp is appended in order to ensure that an existing data is
   * not touched until the transfer has finished. After the transfer has
   * finished, previously existing data can be removed securely.
   *
   * @param relativeDataPath The relative data path used to access the data.
   * @param parentResource The parent data resource.
   * @param properties ApplicationProperties used to obtain the configured data
   * base path.
   *
   * @return The data URI.
   */
  public static URI getDataUri(DataResource parentResource, String relativeDataPath, ApplicationProperties properties){
    try{
      String internalIdentifier = DataResourceUtils.getInternalIdentifier(parentResource);
      if(internalIdentifier == null){
        throw new CustomInternalServerError("Data integrity error. No internal identifier assigned to resource.");
      }
      URIBuilder uriBuilder = new URIBuilder(properties.getBasepath().toURI());
      uriBuilder.setPath(uriBuilder.getPath() + "/" + Calendar.getInstance().get(Calendar.YEAR) + "/" + internalIdentifier + "/" + relativeDataPath + "_" + System.currentTimeMillis());
      return uriBuilder.build();
    } catch(URISyntaxException ex){
      throw new CustomInternalServerError("Failed to transform configured basepath to URI.");
    }
  }
}
