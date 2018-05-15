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

/**
 *
 * @author jejkal
 */
public enum UnknownInformationConstants{

  TEMPORARILY_INACCESSIBLE("(:unac)"),
  UNALLOWED_OR_SUPPRESSED_INTENTIONALLY("(:unal)"),
  NOT_APPLICABLE_OR_MAKES_NO_SENSE("(:unap)"),
  VALUE_UNASSIGNED("(:unas)"),
  VALUE_UNAVAILABLE_OR_POSSIBLY_UNKNOWN("(:unav)"),
  KWOWN_TO_BE_UNKNOWN("(:unkn)"),
  NEVER_HAD_A_VALUE_NEVER_WILL("(:none)"),
  EXPLICITLY_AND_MEANINGFUL_EMPTY("(:null)"),
  TO_BE_ASSIGNED_OR_ANNOUNCED_LATER("(:tba)"),
  TOO_NUMEROUS_TO_LIST("(:etal)");

  private String value;

  private UnknownInformationConstants(String value){
    this.value = value;
  }

  public String getValue(){
    return value;
  }

}
