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
package edu.kit.datamanager.repo.util.test;

import edu.kit.datamanager.repo.domain.ContentInformation;
import edu.kit.datamanager.repo.domain.DataResource;
import edu.kit.datamanager.repo.domain.GeoLocation;
import edu.kit.datamanager.repo.domain.Point;
import edu.kit.datamanager.repo.domain.ResourceType;
import edu.kit.datamanager.repo.domain.Title;
import edu.kit.datamanager.repo.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jejkal
 */
public class EntityUtilsTest {

    @Test
    public void testRemoveIdsFromDataResource() {
        DataResource res = DataResource.factoryDataResourceWithDoi("test123");
        res.setPublisher("test");
        //test top-level @Id
        res.setId("123");

        //test attribute-level @Id
        ResourceType ty = ResourceType.createResourceType("test", ResourceType.TYPE_GENERAL.EVENT);
        ty.setId(1l);
        res.setResourceType(ty);

        //test collection-level @Id
        Title t = Title.factoryTitle("title");
        t.setId(1l);
        res.getTitles().add(t);
        res.getSizes().add("12345");

        //test collection-attribute-level @Id
        Point p = Point.factoryPoint(1f, 1f);
        p.setId(1l);
        GeoLocation loc = GeoLocation.factoryGeoLocation(p);
        loc.setId(1l);
        res.getGeoLocations().add(loc);

        EntityUtils.removeIds(res);

        //top-level @Id should be null
        Assert.assertNull(res.getId());
        //attribute-level @Id should be null
        Assert.assertNull(res.getResourceType().getId());
        //collection-level @Id should be null
        Assert.assertNull(res.getTitles().iterator().next().getId());
        //collection-attribute-level @Id should be null
        Assert.assertNull(res.getGeoLocations().iterator().next().getPoint().getId());
    }

    @Test
    public void testRemoveIdsFromContentInformation() {
        ContentInformation info = ContentInformation.createContentInformation("data.json");
        info.setId(1l);

        EntityUtils.removeIds(info);
        //top-level @Id should be null
        Assert.assertNull(info.getId());
    }

}
