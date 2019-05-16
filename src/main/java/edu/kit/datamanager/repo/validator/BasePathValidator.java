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
package edu.kit.datamanager.repo.validator;

import java.net.URISyntaxException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.kit.datamanager.repo.annotations.BasePathURL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author jejkal
 */
public class BasePathValidator implements ConstraintValidator<BasePathURL, java.net.URL>{

  private static final Logger LOGGER = LoggerFactory.getLogger(BasePathValidator.class);

  @Override
  public boolean isValid(java.net.URL value, ConstraintValidatorContext context){
    boolean basePathValid = false;
    try{
      LOGGER.trace("Successfully validated base path URL {}. Checking local path.", value.toURI().toString());
      Path basePath = Paths.get(value.toURI());

      if(!Files.exists(basePath)){
        LOGGER.trace("Base path at {} does not exist. Try creating it.", basePath);
        Path basePathCreated = Files.createDirectories(basePath);
        LOGGER.info("Successfully created base path from URL {} at local folder {}.", value, basePathCreated);
        basePathValid = true;
      } else{
        if(!Files.isWritable(basePath)){
          LOGGER.error("Base path at {} exists, but is not writable.", basePath);
        } else{
          LOGGER.trace("Base path at {} exists and is writable.", basePath);
          basePathValid = true;
        }
      }
    } catch(URISyntaxException ex){
      LOGGER.error("Failed to validate base path property with value " + value + ".", ex);
    } catch(IOException ex){
      LOGGER.error("Failed to create base path for URL " + value + " at local filesystem.", ex);
    }
    return basePathValid;
  }
}
