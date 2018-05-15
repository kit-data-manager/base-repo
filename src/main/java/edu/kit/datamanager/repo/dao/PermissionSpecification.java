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

import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.acl.AclEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 *
 * @author jejkal
 */
public class PermissionSpecification{

  public static Specification<DataResource> andIfPermission(Specification<DataResource> specifications, final List<String> sids, AclEntry.PERMISSION permission){
    specifications = specifications.and(toSpecification(sids, permission));
    return specifications;
  }

  public static Specification<DataResource> toSpecification(final List<String> sids, final AclEntry.PERMISSION permission){

    return (Root<DataResource> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
      query.distinct(true);

      List<AclEntry.PERMISSION> permissions = new ArrayList<>(Arrays.asList(AclEntry.PERMISSION.values()));
      permissions.removeIf((AclEntry.PERMISSION t) -> t.ordinal() < permission.ordinal());

      Join<DataResource, AclEntry> joinOptions = root.join("acls", JoinType.INNER);
      return builder.and(joinOptions.get("sid").in(sids), joinOptions.get("permission").in(permissions));
    };
  }
}
