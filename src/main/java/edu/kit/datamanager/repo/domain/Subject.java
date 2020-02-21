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

import edu.kit.datamanager.annotations.Searchable;
import edu.kit.datamanager.annotations.SecureUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.Entity;
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
@Data
@Schema(description = "A subject of a resource, which can either be free text or a value URI.")
public class Subject{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "The subject value.", required = false)
  private String value;
  @Schema(required = false)
  @OneToOne(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private Scheme scheme;
  @Schema(example = "http://udcdata.info/037278", required = false)
  private String valueUri;
  @Schema(example = "en", required = false)
  private String lang;

  /**
   * Basic factory method.
   *
   * @param value The subject value
   * @param valueUri The subject value uri
   * @param lang The subject value language
   * @param scheme The relation scheme
   *
   * @return A new instance of RelatedIdentifier
   */
  public static Subject factorySubject(String value, String valueUri, String lang, Scheme scheme){
    Subject result = new Subject();
    result.setValue(value);
    result.setValueUri(valueUri);
    result.setLang(lang);
    result.setScheme(scheme);
    return result;
  }
}
