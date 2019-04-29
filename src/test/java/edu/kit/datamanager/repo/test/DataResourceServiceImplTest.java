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
package edu.kit.datamanager.repo.test;

import com.github.fge.jsonpatch.JsonPatch;
import edu.kit.datamanager.entities.PERMISSION;
import edu.kit.datamanager.exceptions.BadArgumentException;
import edu.kit.datamanager.exceptions.PatchApplicationException;
import edu.kit.datamanager.exceptions.ResourceAlreadyExistException;
import edu.kit.datamanager.exceptions.ResourceNotFoundException;
import edu.kit.datamanager.exceptions.UpdateForbiddenException;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.service.IDataResourceService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author jejkal
 */
public class DataResourceServiceImplTest{

  @Test
  public void testDefaultImpl(){
    IDataResourceService dummyService = new IDataResourceService(){
      @Override
      public DataResource create(DataResource resource, String callerPrincipal, String callerFirstName, String callerLastName) throws BadArgumentException, ResourceAlreadyExistException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public DataResource create(DataResource resource, String callerPrincipal) throws BadArgumentException, ResourceAlreadyExistException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public DataResource findByAnyIdentifier(String identifier){
        DataResource result = new DataResource();
        result.setId(identifier);
        return result;
      }

      @Override
      public Page<DataResource> findAll(DataResource example, Pageable pgbl, boolean includeRevoked){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public Page<DataResource> findAllFiltered(DataResource example, List<String> sids, PERMISSION permission, Pageable pgbl, boolean includeRevoked){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public Page<DataResource> findByExample(DataResource example, List<String> callerIdentities, boolean callerIsAdministrator, Pageable pgbl){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public DataResource findById(String string) throws ResourceNotFoundException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public Page<DataResource> findAll(DataResource c, Pageable pgbl){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public DataResource put(DataResource c, DataResource c1, Collection<? extends GrantedAuthority> clctn) throws UpdateForbiddenException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public void patch(DataResource c, JsonPatch jp, Collection<? extends GrantedAuthority> clctn) throws PatchApplicationException, UpdateForbiddenException{
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public void delete(DataResource c){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public Optional<String> getAuditInformationAsJson(String string, Pageable pgbl){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public Health health(){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    DataResource res = dummyService.findByAnyIdentifier("test123", 666l);

    Assert.assertEquals(res.getId(), "test123");
  }

}
