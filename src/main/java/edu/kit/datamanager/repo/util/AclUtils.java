/*
 * Copyright 2019 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jejkal
 */
public class AclUtils{

  public static boolean isSidInPrincipalList(String sid, List<String> principalIds){
    for(String principalId : principalIds){
      Pattern p = Pattern.compile(principalId);
      Matcher m = p.matcher(sid);
      if(m.matches()){
        return true;
      }
    }

    return false;
  }
//
//  public static void main(String[] args){
//
//    List<String> principalIds = new ArrayList<>();
//    principalIds.add("tester");
//    principalIds.add(".*KIT");
//
//    System.out.println(isSidInPrincipalList("USERS@KIT", principalIds));
//    System.out.println(isSidInPrincipalList("tester", principalIds));
//    System.out.println(isSidInPrincipalList("test", principalIds));
//    System.out.println(isSidInPrincipalList("KIT_LEADER", principalIds));
//
//  }
}
