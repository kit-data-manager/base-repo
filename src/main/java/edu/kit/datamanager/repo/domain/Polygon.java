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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Data
@Schema(description = "Geo location information as polygon. A polygon must consist of 4 or more points.")
public class Polygon{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(required = true)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Point> points;

  /**
   * Basic factory method.
   *
   * @param points The points
   *
   * @return A new instance of Polygon
   */
  public static Polygon factoryPolygon(Point... points){
    Polygon result = new Polygon();
    if(points == null || points.length < 3){
      throw new IllegalArgumentException("A polygon is formed by at least three points.");
    }
    result.setPoints(new HashSet<>(Arrays.asList(points)));
    return result;
  }

}
