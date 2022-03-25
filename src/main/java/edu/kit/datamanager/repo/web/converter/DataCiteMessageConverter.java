/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.datamanager.repo.web.converter;

import edu.kit.datamanager.repo.domain.DataResource;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 *
 * @author jejkal
 */
public class DataCiteMessageConverter implements HttpMessageConverter {

    @Autowired
    private Logger LOGGER = LoggerFactory.getLogger(DataCiteMessageConverter.class);

    private Object applyJoltTransformation(Object payload) {
        LOGGER.trace("Reading JOLT spec from /datacite.jolt");
        List chainrSpecJSON = JsonUtils.classpathToList("/datacite.jolt");
        Chainr chainr = Chainr.fromSpec(chainrSpecJSON);
        LOGGER.trace("Transforming input string {} to object.", payload);
        Object result = chainr.transform(JsonUtils.jsonToObject((String) payload));
        try {
            LOGGER.trace("Converting transformation result {} to DataResource.", result);
            return new ObjectMapper().readValue((String) JsonUtils.toJsonString(result), DataResource.class);
        } catch (JsonProcessingException ex) {
            LOGGER.error("Failed to transform JOLT result to DataResource.", ex);
        }
        return null;
    }

    @Override
    public boolean canRead(Class arg0, MediaType arg1) {
        if (arg0 == null || arg1 == null) {
            return false;
        }
        LOGGER.trace("Checking applicability of DataCiteMessageConverter for class {} and mediatype {}.", arg0, arg1);
        return DataResource.class.equals(arg0) && arg1.toString().startsWith("application/json+datacite");
    }

    @Override
    public boolean canWrite(Class arg0, MediaType arg1) {
        //writing currently not supported
        return false;
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.valueOf("application/json+datacite"));
    }

    @Override
    public Object read(Class arg0, HttpInputMessage arg1) throws IOException, HttpMessageNotReadableException {
        LOGGER.trace("Resing HttpInputMessage for JOLT transformation.");
        String data = new BufferedReader(new InputStreamReader(arg1.getBody(), UTF_8)).lines().collect(Collectors.joining("\n"));' or 'String data = new BufferedReader(new InputStreamReader(arg1.getBody(), Charset.defaultCharset())).lines().collect(Collectors.joining("\n"));
        return applyJoltTransformation(data);
    }

    @Override
    public void write(Object arg0, MediaType arg1, HttpOutputMessage arg2) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
