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
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.client.utils.URIBuilder;

/**
 *
 * @author jejkal
 */
public class PathUtils {

    private PathUtils() {
    }

    /**
     * Obtain the absolute data uri for the provided data resource and relative
     * data path. The final URI will contain the relative data data path
     * appended to a base URI depending on the repository configuration. In
     * addition, the current timestamp is appended in order to ensure that an
     * existing data is not touched until the transfer has finished. After the
     * transfer has finished, previously existing data can be removed securely.
     *
     * @param relativeDataPath The relative data path used to access the data.
     * @param parentResource The parent data resource.
     * @param properties ApplicationProperties used to obtain the configured
     * data base path.
     *
     * @return The data URI.
     */
    public static URI getDataUri(DataResource parentResource, String relativeDataPath, ApplicationProperties properties) {
        try {
            String internalIdentifier = DataResourceUtils.getInternalIdentifier(parentResource);
            if (internalIdentifier == null) {
                throw new CustomInternalServerError("Data integrity error. No internal identifier assigned to resource.");
            }

            URIBuilder uriBuilder = new URIBuilder(properties.getBasepath().toString());
            uriBuilder.setPath(uriBuilder.getPath() + (!properties.getBasepath().toString().endsWith("/") ? "/" : "") + substitutePathPattern(properties) + "/" + internalIdentifier + "/" + relativeDataPath + "_" + System.currentTimeMillis());
            return uriBuilder.build();
        } catch (URISyntaxException ex) {
            throw new CustomInternalServerError("Failed to transform configured basepath to URI.");
        }
    }

    public static String substitutePathPattern(ApplicationProperties properties) {
        Map<String, String> data = new HashMap<>();
        data.put("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
        data.put("month", Integer.toString(Calendar.getInstance().get(Calendar.MONTH)));
        data.put("day", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));

        String pattern = properties.getPathPattern().replaceAll("\\@", "\\$");
        if (pattern.startsWith("/")) {
            pattern = pattern.substring(1);
        }
        if (pattern.endsWith("/")) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        return StringSubstitutor.replace(pattern, data);
    }

    public static String normalizePath(String path) {
        return normalizePath(path, true);
    }

    public static String normalizePath(String path, boolean removeOuterSlashes) {
        String normalizedPath = path.replaceAll("/+", "/");
        if (removeOuterSlashes) {
            //remove leading slash
            normalizedPath = normalizedPath.startsWith("/") ? normalizedPath.substring(1) : normalizedPath;
            //remove trailing slash
            normalizedPath = normalizedPath.endsWith("/") ? normalizedPath.substring(0, normalizedPath.length() - 1) : normalizedPath;
        }
        return normalizedPath;
    }

    public static int getDepth(String relativePath) {
        String normalizedPath = PathUtils.normalizePath(relativePath);
        return normalizedPath.split("/").length;
    }
}
