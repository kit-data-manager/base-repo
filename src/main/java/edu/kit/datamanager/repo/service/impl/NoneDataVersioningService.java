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
package edu.kit.datamanager.repo.service.impl;

import edu.kit.datamanager.entities.VersionInfo;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.util.PathUtils;
import edu.kit.datamanager.service.IVersioningService;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author jejkal
 */
@Component
public class NoneDataVersioningService implements IVersioningService{

  @Autowired
  private Logger logger;
  @Autowired
  private IContentInformationDao dao;
  @Autowired
  private ApplicationProperties applicationProperties;

  @Override
  public void configure(){

  }

  @Override
  public void write(String resourceId, String callerId, String path, InputStream stream, Map<String, String> map){
    boolean force = Boolean.parseBoolean(map.get("force"));
    String contentUriString = map.get("contentUri");

    if(contentUriString != null){
      logger.trace("ContentUri option provided, checking 'force' option.");
      if(!force){
        //conflict
        logger.error("Existing content information found for resource {} at path {} and 'force' flag not set. Throwing ResourceAlreadyExistException.", "DataResource#" + resourceId, path);
        throw new ResourceAlreadyExistException("There is already content registered at " + path + ". Provide force=true in order to replace the existing resource.");
      } else{
        logger.trace("'Force' flag set. Checking for local content to replace.");
        //overwrite...mark file for deletion
        URI contentUri = URI.create(contentUriString);
        if(!"file".equals(contentUri.getScheme())){
          //mark file for removal
          logger.debug("Existing contentUri {} is no local file. Creating new contentUri.", contentUri);
          // toRemove = Paths.get(contentUri);
          contentUri = PathUtils.getDataUri(DataResource.factoryNewDataResource(resourceId), path, applicationProperties);
          contentUriString = contentUri.toString();
        }
      }
    } else{
      logger.trace("New entry detected, setting initial contentUri.");
      contentUriString = PathUtils.getDataUri(DataResource.factoryNewDataResource(resourceId), path, applicationProperties).toString();
    }

    logger.trace("Obtaining path for contentUriString {}.", contentUriString);
    //URI dataUri = PathUtils.getDataUri(DataResource.factoryNewDataResource(resourceId), path, applicationProperties);
    Path destination = Paths.get(URI.create(contentUriString));
    logger.trace("Preparing destination {} for storing user data.", destination);
    //store data
    OutputStream out = null;
    try{
      //read/write file, create checksum and calculate file size
      Files.createDirectories(destination.getParent());

      MessageDigest md = MessageDigest.getInstance("SHA1");
      logger.trace("Start reading user data from stream.");
      int cnt;
      long bytes = 0;
      byte[] buffer = new byte[1024];
      out = Files.newOutputStream(destination);
      while((cnt = stream.read(buffer)) > -1){
        out.write(buffer, 0, cnt);
        md.update(buffer, 0, cnt);
        bytes += cnt;
      }

      logger.trace("Performing upload post-processing.");
      map.put("checksum", "sha1:" + Hex.encodeHexString(md.digest()));
      logger.debug("Assigned hash {} to content information.", map.get("checksum"));
      map.put("size", Long.toString(bytes));
      logger.debug("Assigned size {} to content information.", map.get("size"));
      map.put("contentUri", contentUriString);
      logger.debug("Assigned content URI {} to content information.", map.get("contentUri"));

      if(!map.containsKey("mediaType")){
        logger.trace("Trying to determine content type.");
        try(InputStream is = Files.newInputStream(destination); BufferedInputStream bis = new BufferedInputStream(is);){
          AutoDetectParser parser = new AutoDetectParser();
          Detector detector = parser.getDetector();
          Metadata md1 = new Metadata();
          //md1.add(Metadata.RESOURCE_NAME_KEY, contentInfo.getFilename());
          org.apache.tika.mime.MediaType mediaType = detector.detect(bis, md1);
          map.put("mediaType", mediaType.toString());
          logger.trace("Assigned media type {} to content information.", map.get("mediaType"));
        }
      }
    } catch(IOException ex){
      logger.error("Failed to finish upload. Throwing CustomInternalServerError.", ex);
      throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
    } catch(NoSuchAlgorithmException ex){
      logger.error("Failed to initialize SHA1 message digest. Throwing CustomInternalServerError.", ex);
      throw new CustomInternalServerError("Internal digest initialization error. Unable to perform upload.");
    } finally{
      if(out != null){
        try{
          out.flush();
          out.close();
        } catch(IOException ignored){
        }
      }
    }
  }

  @Override
  public void read(String resourceId, String callerId, String path, String versionId, OutputStream destination, Map<String, String> options){
    String contentUriString = options.get("contentUri");
    logger.trace("Checking URI {}.", contentUriString);

    System.out.println("CONTEN " + contentUriString);
    System.out.println(Files.exists(Paths.get(URI.create(contentUriString))));
    try{
      System.out.println(Files.list(Paths.get(URI.create(contentUriString)).getParent()));
    } catch(IOException ex){
      ex.printStackTrace();
    }

    if(!Files.exists(Paths.get(URI.create(contentUriString)))){
      logger.error("Content at URI {} seems not to exist.", contentUriString);
      throw new ResourceNotFoundException("The provided resource was not found on the server.");
    } else{
      logger.trace("Content URI at {} found.", contentUriString);
    }

    try{
      logger.trace("Copying file content to target stream.");
      Files.copy(Paths.get(URI.create(contentUriString)), destination);
    } catch(IOException ex){
      logger.error("Failed to read content stream.", ex);
      throw new CustomInternalServerError("Failed to read content stream.");
    }
  }

  @Override
  public VersionInfo info(String resourceId, String path, String versionId, Map<String, String> options){
    return new VersionInfo(resourceId, versionId, null, null, null, null, null, new HashSet<>(Arrays.asList(path)));
  }

  @Override
  public String getServiceName(){
    return "none";
  }

}
