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
package edu.kit.datamanager.repo.service.impl;

import edu.kit.datamanager.entities.RepoUserRole;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.CustomInternalServerError;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.repo.configuration.ApplicationProperties;
import edu.kit.datamanager.repo.dao.ContentInformationMatchSpecification;
import edu.kit.datamanager.repo.dao.ContentInformationTagSpecification;
import edu.kit.datamanager.repo.dao.IContentInformationDao;
import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IContentInformationService;
import edu.kit.datamanager.repo.util.PathUtils;
import edu.kit.datamanager.util.AuthenticationHelper;
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
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.apache.commons.codec.binary.Hex;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author jejkal
 */
public class ContentInformationService implements IContentInformationService{

  @Autowired
  private Logger logger;

  @Autowired
  private IContentInformationDao dao;

  @PersistenceContext
  private EntityManager em;
  @Autowired
  private ApplicationProperties applicationProperties;

  @Override
  @Transactional(readOnly = true)
  public Optional<ContentInformation> findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(Long id, String relativePath, String tag){

    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(id, relativePath, true));//new ByExampleSpecification(em).byExample(example));
    if(tag != null){
      spec = ContentInformationTagSpecification.andIfPermission(spec, tag);
    }

    return dao.findOne(spec);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ContentInformation> findByParentResourceIdEqualsAndRelativePathLikeAndHasTag(Long id, String relativePath, String tag, Pageable pgbl){
    Specification<ContentInformation> spec = Specification.where(ContentInformationMatchSpecification.toSpecification(id, relativePath, false));
    if(tag != null){
      spec = ContentInformationTagSpecification.andIfPermission(spec, tag);
    }
    return dao.findAll(spec, pgbl);
  }

//  @Override
//  @Transactional(readOnly = true)
//  public Page<ContentInformation> findAll(ContentInformation example, Pageable pgbl){
//    if(example != null){
//      Specification<ContentInformation> spec = Specification.where(new ByExampleSpecification(em).byExample(example));
//      return getDao().findAll(spec, pgbl);
//    }
//    return getDao().findAll(pgbl);
//  }
  @Override
  public ContentInformation create(ContentInformation contentInformation, DataResource resource, InputStream file, String path, boolean force){
    //check for existing content information
    ContentInformation contentInfo;

    Optional<ContentInformation> existingContentInformation = findByParentResourceIdEqualsAndRelativePathEqualsAndHasTag(resource.getId(), path, null);

    Path toRemove = null;
    if(existingContentInformation.isPresent()){
      //existing content, overwrite necessary
      if(!force){
        //conflict
        throw new ResourceAlreadyExistException("There is already content registered at " + path + ". Provide force=true in order to overwrite the existing resource.");
      } else{
        //overwrite...mark file for deletion
        contentInfo = existingContentInformation.get();
        URI contentUri = URI.create(contentInfo.getContentUri());
        if("file".equals(contentUri.getScheme())){
          //mark file for removal
          toRemove = Paths.get(URI.create(contentInfo.getContentUri()));
        }//content URI is not pointing to a file...just replace the entry
      }
    } else{
      //no existing content information, create new or take provided
      contentInfo = (contentInformation != null) ? contentInformation : ContentInformation.createContentInformation(path);
      contentInfo.setId(null);
      contentInfo.setParentResource(resource);
      contentInfo.setRelativePath(path);
    }

    if(file != null){
      //file upload
      URI dataUri = PathUtils.getDataUri(contentInfo.getParentResource(), contentInfo.getRelativePath(), applicationProperties);
      Path destination = Paths.get(dataUri);
      //store data
      OutputStream out = null;
      try{
        //read/write file, create checksum and calculate file size
        Files.createDirectories(destination.getParent());

        MessageDigest md = MessageDigest.getInstance("SHA1");

        int cnt;
        long bytes = 0;
        byte[] buffer = new byte[1024];
        out = Files.newOutputStream(destination);
        while((cnt = file.read(buffer)) > -1){
          out.write(buffer, 0, cnt);
          md.update(buffer, 0, cnt);
          bytes += cnt;
        }

        contentInfo.setHash("sha1:" + Hex.encodeHexString(md.digest()));
        contentInfo.setSize(bytes);
        contentInfo.setContentUri(dataUri.toString());

        try(InputStream is = Files.newInputStream(destination); BufferedInputStream bis = new BufferedInputStream(is);){
          AutoDetectParser parser = new AutoDetectParser();
          Detector detector = parser.getDetector();
          Metadata md1 = new Metadata();
          md1.add(Metadata.RESOURCE_NAME_KEY, contentInfo.getFilename());
          org.apache.tika.mime.MediaType mediaType = detector.detect(bis, md1);
          contentInfo.setMediaType(mediaType.toString());
        }
      } catch(IOException ex){
        logger.error("Failed to finish upload.", ex);
        throw new CustomInternalServerError("Unable to read from stream. Upload canceled.");
      } catch(NoSuchAlgorithmException ex){
        logger.error("Failed to initialize SHA1 message digest. File upload won't be possible.", ex);
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
    } else{
      //no file upload, take data reference URI from provided content information
      if(contentInformation == null || contentInformation.getContentUri() == null){
        throw new BadArgumentException("Neither a file upload nor an external content URI were provided.");
      } else{
        if("file".equals(URI.create(contentInfo.getContentUri()).getScheme().toLowerCase()) && !AuthenticationHelper.hasAuthority(RepoUserRole.ADMINISTRATOR.toString())){
          throw new BadArgumentException("You are not permitted to add content information with URI scheme of type 'file'.");
        }
        //take content uri and provided checksum and size, if available
        contentInfo.setContentUri(contentInformation.getContentUri());
        contentInfo.setSize(contentInformation.getSize());
        contentInfo.setHash(contentInformation.getHash());
      }
    }

    //copy metadata and tags from provided content information if available
    if(contentInformation != null){
      if(contentInformation.getMetadata() != null){
        contentInfo.setMetadata(contentInformation.getMetadata());
      }
      if(contentInformation.getTags() != null){
        contentInfo.setTags(contentInformation.getTags());
      }
    }

    ContentInformation result = getDao().save(contentInfo);
    if(toRemove != null){
      try{
        Files.deleteIfExists(toRemove);
      } catch(IOException ex){
        logger.warn("Failed to remove previously existing data at " + toRemove + ". Manual removal required.", ex);
      }
    }
    return result;

  }

  @Override
  public ContentInformation createOrUpdate(ContentInformation entity){
    return getDao().save(entity);
  }

  @Override
  public void delete(ContentInformation entity){
    getDao().delete(entity);
  }

  protected IContentInformationDao getDao(){
    return dao;
  }

  @Override
  public Health health(){
    return Health.up().withDetail("ContentInformation", dao.count()).build();
  }
}
