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
package edu.kit.datamanager.repo.dao;

import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class ContentInformationMatchSpecification{

  public static Specification<ContentInformation> andIfPermission(Specification<ContentInformation> specifications, final String parentId, final String path, boolean exactPath){
    specifications = specifications.and(toSpecification(parentId, path, exactPath));
    return specifications;
  }

  public static Specification<ContentInformation> toSpecification(final String parentId, final String path, final boolean exactPath){
//
//    ExampleMatcher m = ExampleMatcher.matching().withMatcher("tst1234", ExampleMatcher.GenericPropertyMatchers.endsWith());
//        Example<DataResource> ex = Example.of(DataResource.factoryDataResourceWithDoi("test123"), m);

    return (Root<ContentInformation> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      Join<ContentInformation, DataResource> joinOptions = root.join("parentResource");

      Path<DataResource> p = root.get("parentResource");
      Path<String> pid = p.get("id");

      if(!exactPath){
        return builder.and(builder.equal(pid, parentId), builder.like(root.get("relativePath"), path));
      } else{
        return builder.and(builder.equal(pid, parentId), builder.equal(root.get("relativePath"), path));
      }
    };
  }
}
