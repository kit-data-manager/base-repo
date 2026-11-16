/*
 * Copyright 2025 Karlsruhe Institute of Technology.
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

import edu.kit.datamanager.repo.configuration.MonitoringConfiguration;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.dao.IDataResourceDao;
import io.micrometer.common.lang.NonNull;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BaseRepoMonitoringService implements MeterBinder {
    /**
     * Prefix for metrics.
     */
    public static String PREFIX_METRICS = "base-repo.";
    /**
     * Label for metrics of data resources.
     */
    public static final String LABEL_DATA_RESOURCES = "_data_resources";
    /**
     * Label for metrics of content information.
     */
    public static final String LABEL_CONTENT_INFORMATION = "_content_information";

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BaseRepoMonitoringService.class);
    private final IDataResourceDao dataResourceDao;
    private final IContentInformationDao contentInformationDao;
    private final MonitoringConfiguration monitoringConfiguration;

    /**
     * Constructor.
     *
     * @param monitoringConfiguration Configuration for monitoring.
     */
    public BaseRepoMonitoringService(@org.springframework.lang.NonNull MonitoringConfiguration monitoringConfiguration,
                                     @org.springframework.lang.NonNull IDataResourceDao dataResourceDao,
                                     @org.springframework.lang.NonNull IContentInformationDao contentInformationDao) {
        this.monitoringConfiguration = monitoringConfiguration;
        this.dataResourceDao = dataResourceDao;
        this.contentInformationDao = contentInformationDao;
        PREFIX_METRICS = monitoringConfiguration.getServiceName();
    }

    @Override
    public void bindTo(@NonNull MeterRegistry meterRegistry) {
        if (monitoringConfiguration.isEnabled()) {
            Gauge.builder(PREFIX_METRICS + LABEL_DATA_RESOURCES, this::countDataResources).register(meterRegistry);
            Gauge.builder(PREFIX_METRICS + LABEL_CONTENT_INFORMATION, this::countContentInformation).register(meterRegistry);
        } else {
            LOG.info("Monitoring is disabled. Skipping metric registration.");
        }
    }

    /**
     * Count the number of data resources.
     *
     * @return The number of data resources.
     */
    long countDataResources() {
        return dataResourceDao.count();

    }

    /**
     * Count the number of files.
     *
     * @return The number of files.
     */
    long countContentInformation() {
        return contentInformationDao.count();
    }
}
