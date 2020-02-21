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
import lombok.Data;

/**
 *
 * @author jejkal
 */
@Entity
@Data
@Schema(description = "Geo location information as point.")
public class Point{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(example = "-180 <= longitude <= 180", required = true)
  private float longitude;
  @Schema(example = "-90 <= latitude <= 90", required = true)
  private float latitude;

  /**
   * Basic factory method.
   *
   * @param longitute The longitude
   * @param latitude The latitude
   *
   * @return A new instance of Point
   */
  public static Point factoryPoint(float longitute, float latitude){
    Point result = new Point();
    result.setLongitude(longitute);
    result.setLatitude(latitude);
    return result;
  }
}
