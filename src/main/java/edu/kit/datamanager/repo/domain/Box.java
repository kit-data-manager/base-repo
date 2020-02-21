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
@Schema(description = "Geo location information as box.")
@Data
public class Box{

  @Id
  @Schema(required = false, accessMode = Schema.AccessMode.READ_ONLY)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @SecureUpdate({"FORBIDDEN"})
  @Searchable
  private Long id;
  @Schema(description = "-67.302", example = "-180 <= westLongitude <= 180", required = true)
  private float westLongitude;
  @Schema(description = "-67.302", example = "-180 <= eastLongitude <= 180", required = true)
  private float eastLongitude;
  @Schema(description = "31.233", example = "-90 <= southLatitude <= 90", required = true)
  private float southLatitude;
  @Schema(description = "31.233", example = "-90 <= northLatitude <= 90", required = true)
  private float northLatitude;

  /**
   * Basic factory method.
   *
   * @param westLongitude The west longitude
   * @param eastLongitude The east longitude
   * @param southLatitude The east latitude
   * @param northLatitude The east latitude
   *
   * @return A new instance of Box
   */
  public static Box factoryBox(float westLongitude, float eastLongitude, float southLatitude, float northLatitude){
    Box result = new Box();
    result.setWestLongitude(westLongitude);
    result.setEastLongitude(eastLongitude);
    result.setNorthLatitude(northLatitude);
    result.setSouthLatitude(southLatitude);
    return result;
  }

  /**
   * Basic factory method.
   *
   * @param upperLeftCoordinate The upper left coordinate
   * @param lowerRightCoordinate The lower right coordinate
   *
   * @return A new instance of Box
   */
  public static Box factoryBox(Point upperLeftCoordinate, Point lowerRightCoordinate){
    Box result = new Box();
    result.setWestLongitude(upperLeftCoordinate.getLongitude());
    result.setEastLongitude(lowerRightCoordinate.getLongitude());
    result.setNorthLatitude(upperLeftCoordinate.getLatitude());
    result.setSouthLatitude(lowerRightCoordinate.getLatitude());
    return result;
  }

}
