/*
 * Copyright 2023 Karlsruhe Institute of Technology.
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

import jakarta.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.Set;

/**
 *
 * @author jejkal
 */
public class EntityUtils {

    public static void removeIds(Object originalObj) {
        if (originalObj == null) {
            System.out.println("NULL!");
            return;
        }
        for (Field field : originalObj.getClass().getDeclaredFields()) {
            System.out.println("NEW FIELD " + field);
            Id idField = field.getAnnotation(Id.class);
            if (idField != null) {
                //field is id field
                field.setAccessible(true);
                try {
                    if (field.get(originalObj) != null) {
                        //id field is set
                        System.out.println("REMOVE ID " + field);
                        field.set(originalObj, null);
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    //update failed
                    System.out.println("NO ACCESS");
                }
            } else if (Set.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    Object val = field.get(originalObj);
                    if (val != null) {
                        for (Object v : (Set) val) {
                            removeIds(v);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    //update failed
                    System.out.println("NO ACCESS");
                }
            } else if (!field.getType().isPrimitive() && !field.getType().isEnum() && field.getType().getPackageName().startsWith("edu.kit.datamanager")) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(originalObj);
                    if (val != null) {
                        removeIds(val);
                    }
                } catch (IllegalArgumentException | IllegalAccessException | InaccessibleObjectException ex) {
                    //update failed
                    System.out.println("NO ACCESS");
                }
            }
        }
    }
}
