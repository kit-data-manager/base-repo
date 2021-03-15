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
package edu.kit.datamanager.repo.configuration;

import edu.kit.datamanager.configuration.GenericApplicationProperties;
import java.net.URL;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author jejkal
 */
@Component
@Data
@Validated
@RefreshScope
@EqualsAndHashCode(callSuper = true)
public class ApplicationProperties extends GenericApplicationProperties {

    @edu.kit.datamanager.annotations.LocalFolderURL
    @Value("${repo.basepath}")
    private URL basepath;

    @Value("${repo.readonly:FALSE}")
    private boolean readOnly;

    @Value("${repo.audit.enabled:FALSE}")
    private boolean auditEnabled;
    @Value("${repo.basepath.pattern:'@{year}'}")
    private String pathPattern;
    @Value("${repo.plugin.versioning:none}")
    private String defaultVersioningService;
    @Value("${repo.plugin.storage:dateBased}")
    private String defaultStorageService;
}
