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
import java.util.Optional;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author jejkal
 */
public interface IContentInformationService extends HealthIndicator{

  public Optional<ContentInformation> findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(Long id, String relativePath, String tag);

  public Page<ContentInformation> findByParentResourceIdEqualsAndRelativePathLikeAndHasTag(Long id, String relativePath, String tag, Pageable pgbl);

  public Page<ContentInformation> findAll(ContentInformation example, Pageable pgbl);

  ContentInformation create(final ContentInformation entity);

  ContentInformation update(final ContentInformation entity);

  void delete(final ContentInformation entity);
}
