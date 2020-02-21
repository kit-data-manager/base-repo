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
import edu.kit.datamanager.entities.BaseEnum;
import edu.kit.datamanager.entities.Identifier;
import edu.kit.datamanager.util.EnumUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity()
@Data
@Schema(description = "A related identifier for a resource.")
public class RelatedIdentifier{

  public enum RELATION_TYPES implements BaseEnum{
    IS_CITED_BY("IsCitedBy"),
    CITES("Cites"),
    IS_SUPPLEMENT_TO("IsSupplementTo"),
    IS_SUPPLEMENTED_BY("IsSupplementedBy"),
    IS_CONTINUED_BY("IsContinuedBy"),
    CONTINUES("Continues"),
    IS_NEW_VERSION_OF("IsNewVersionOf"),
    IS_PREVIOUS_VERSION_OF("IsPreviousVersionOf"),
    IS_PART_OF("IsPartOf"),
    HAS_PART("HasPart"),
    IS_REFERENCED_BY("IsReferencedBy"),
    REFERENCES("References"),
    IS_DOCUMENTED_BY("IsDocumentedBy"),
    DOCUMENTS("Documents"),
    IS_COMPILED_BY("IsCompiledBy"),
    COMPILES("Compiles"),
    IS_VARIANT_FORM_OF("IsVariantFormOf"),
    IS_ORIGINAL_FORM_OF("IsOriginalFormOf"),
    IS_IDENTICAL_TO("IsIdenticalTo"),
    HAS_METADATA("HasMetadata"),
    IS_METADATA_FOR("IsMetadataFor"),
    REVIEWS("Reviews"),
    IS_REVIEWED_BY("IsReviewedBy"),
    IS_DERIVED_FROM("IsDerivedFrom"),
    IS_SOURCE_OF("IsSourceOf");

    private final String value;

    private RELATION_TYPES(String value){
      this.value = value;
    }

    @Override
    public String getValue(){
      return value;
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "Controlled vocabulary, e.g. INTERNAL or DOI.", required = true)
  @Enumerated(EnumType.STRING)
  private Identifier.IDENTIFIER_TYPE identifierType;
  @Schema(description = "10.1234/foo", required = true)
  private String value;
  //vocab, e.g. IsMetadataFor...
  @Schema(description = "Controlled vocabulary value describing the relation type, e.g. IS_PART_OF or IS_METADATA_FOR.", required = true)
  @Enumerated(EnumType.STRING)
  private RELATION_TYPES relationType;
  @Schema(description = "Identifier scheme.", required = false)
  @OneToOne(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
  private Scheme scheme;
  @Schema(description = "Related metadata scheme.", required = false)
  private String relatedMetadataScheme;

  /**
   * Basic factory method.
   *
   * @param type The relation type
   * @param value The identifier value
   * @param scheme The relation scheme
   * @param metadataScheme The relation metadata scheme
   *
   * @return A new instance of RelatedIdentifier
   */
  public static RelatedIdentifier factoryRelatedIdentifier(RELATION_TYPES type, String value, Scheme scheme, String metadataScheme){
    RelatedIdentifier result = new RelatedIdentifier();
    result.setRelationType(type);
    result.setValue(value);
    result.setScheme(scheme);
    result.setRelatedMetadataScheme(metadataScheme);
    return result;
  }

  @Override
  public boolean equals(Object obj){
    if(this == obj){
      return true;
    }
    if(obj == null){
      return false;
    }
    if(getClass() != obj.getClass()){
      return false;
    }
    final RelatedIdentifier other = (RelatedIdentifier) obj;
    if(!Objects.equals(this.getId(), other.getId())){
      return false;
    }

    if(!Objects.equals(this.getValue(), other.getValue())){
      return false;
    }
    if(!Objects.equals(this.getScheme(), other.getScheme())){
      return false;
    }
    if(!Objects.equals(this.relatedMetadataScheme, other.relatedMetadataScheme)){
      return false;
    }
    return EnumUtils.equals(this.relationType, other.relationType);
  }

  @Override
  public int hashCode(){
    int hash = 5;
    hash = 67 * hash + Objects.hashCode(this.getId());
    hash = 67 * hash + Objects.hashCode(this.getValue());
    hash = 67 * hash + Objects.hashCode(this.scheme);
    hash = 67 * hash + Objects.hashCode(this.relatedMetadataScheme);
    hash = 67 * hash + EnumUtils.hashCode(this.relationType);
    return hash;
  }
}
