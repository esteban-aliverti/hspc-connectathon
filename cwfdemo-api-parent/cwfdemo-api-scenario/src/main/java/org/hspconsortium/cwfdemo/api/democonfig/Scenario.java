/*
 * #%L
 * Demo Configuration Plugin
 * %%
 * Copyright (C) 2014 - 2016 Healthcare Services Platform Consortium
 * %%
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
 * #L%
 */
package org.hspconsortium.cwfdemo.api.democonfig;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.common.DateUtil;
import org.carewebframework.common.MiscUtil;
import org.hl7.fhir.dstu3.model.BaseDateTimeType;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.BaseService;
import org.hspconsortium.cwf.fhir.common.FhirUtil;

import ca.uhn.fhir.parser.IParser;

public class Scenario {
    
    private static final Log log = LogFactory.getLog(Scenario.class);
    
    private final List<IBaseResource> resourceList = new ArrayList<>();
    
    private final ScenarioDefinition scenarioDefinition;
    
    private boolean initialized;
    
    public Scenario(ScenarioDefinition scenarioDefinition) {
        this.scenarioDefinition = scenarioDefinition;
    }
    
    public String getName() {
        return scenarioDefinition.getName();
    }
    
    public List<IBaseResource> getResources() {
        return Collections.unmodifiableList(resourceList);
    }
    
    public Scenario init() {
        if (initialized) {
            destroy();
        }
        
        BaseService fhirService = scenarioDefinition.getFhirService();
        IParser jsonParser = fhirService.getClient().getFhirContext().newJsonParser();
        Map<String, Map<String, String>> config = scenarioDefinition.getConfig();
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        
        for (String name : config.keySet()) {
            Map<String, String> map = config.get(name);
            String source = map.get("source");
            
            if (source == null) {
                throw new RuntimeException("No source specified in scenario.");
            }
            
            IBaseResource resource = parseResource(source, map, jsonParser, resourceMap);
            scenarioDefinition.addTags(resource);
            resource = fhirService.createOrUpdateResource(resource);
            resourceList.add(resource);
            logAction(resource, "Created");
            resourceMap.put(name, resource);
        }
        
        initialized = true;
        return this;
    }
    
    public void load() {
        if (!initialized) {
            loadResources(false);
        }
    }
    
    /**
     * Load all resources for this scenario.
     */
    @SuppressWarnings("unchecked")
    private void loadResources(boolean silent) {
        initialized = true;
        BaseService fhirService = scenarioDefinition.getFhirService();
        IBaseCoding scenarioTag = scenarioDefinition.getTag();
        resourceList.clear();
        
        for (Class<? extends IBaseResource> clazz : ScenarioUtil.getResourceClasses()) {
            for (IBaseResource resource : fhirService.searchResourcesByTag(scenarioTag, (Class<IBaseResource>) clazz)) {
                resourceList.add(resource);
                
                if (!silent) {
                    logAction(resource, "Retrieved");
                }
            }
        }
    }
    
    public int destroy() {
        loadResources(true);
        int count = 0;
        boolean deleted = true;
        BaseService fhirService = scenarioDefinition.getFhirService();
        
        while (deleted) {
            deleted = false;
            
            for (int i = resourceList.size() - 1; i >= 0; i--) {
                IBaseResource resource = resourceList.get(i);
                
                try {
                    fhirService.deleteResource(resource);
                    resourceList.remove(i);
                    deleted = true;
                    count++;
                    logAction(resource, "Deleted");
                } catch (Exception e) {}
            }
        }
        
        for (IBaseResource resource : resourceList) {
            logAction(resource, "Failed to delete");
        }
        
        resourceList.clear();
        initialized = false;
        return count;
    }
    
    private void logAction(IBaseResource resource, String operation) {
        FhirUtil.stripVersion(resource);
        log.info(operation + " resource: " + resource.getIdElement().getValue());
    }
    
    private IBaseResource parseResource(String source, Map<String, String> map, IParser jsonParser,
                                        Map<String, IBaseResource> resourceMap) {
        source = addExtension(source, "json");
        StringBuilder sb = new StringBuilder();
        
        try (InputStream is = scenarioDefinition.getResourceAsStream(source);) {
            List<String> json = IOUtils.readLines(is, "UTF-8");
            
            for (String s : json) {
                int p1;
                
                while ((p1 = s.indexOf("${")) > -1) {
                    int p2 = s.indexOf("}", p1);
                    String key = s.substring(p1 + 2, p2);
                    String value = map.get(key);
                    
                    if (value == null) {
                        throw new RuntimeException("Reference not found: " + key);
                    }
                    
                    String r = eval(value, resourceMap);
                    s = s.substring(0, p1) + r + s.substring(p2 + 1);
                }
                
                sb.append(s).append('\n');
            }
        } catch (Exception e) {
            MiscUtil.toUnchecked(e);
        }
        
        return jsonParser.parseResource(sb.toString());
    }
    
    /**
     * Add default extension if one is not present.
     * 
     * @param source File resource path.
     * @param dflt The default extension.
     * @return File resource path with extension.
     */
    private String addExtension(String source, String dflt) {
        return source.contains(".") ? source : source + "." + dflt;
    }
    
    /**
     * Evaluate an expression.
     * 
     * @param exp The expression. The general format is
     *            <p>
     *            <code>type/value</code>
     *            </p>
     *            If <code>type</code> is omitted, it is assumed to be a placeholder for a resource
     *            previously defined. Possible values for <code>type</code> are:
     *            <ul>
     *            <li>value - A literal value; inserted as is</li>
     *            <li>date - A date value; can be a relative date (T+n, for example)</li>
     *            <li>image - A file containing an image</li>
     *            <li>snippet - A file containing a snippet to be inserted</li>
     *            </ul>
     * @param resourceMap Map of resolved resources.
     * @return The result of the evaluation.
     */
    private String eval(String exp, Map<String, IBaseResource> resourceMap) {
        int i = exp.indexOf('/');
        
        if (i == -1) {
            IBaseResource resource = resourceMap.get(exp);
            
            if (resource == null) {
                throw new RuntimeException("Resource not defined: " + exp);
            }
            
            return resource.getIdElement().getResourceType() + "/" + resource.getIdElement().getIdPart();
        }
        
        String type = exp.substring(0, i);
        String value = exp.substring(i + 1);
        
        if ("value".equals(type)) {
            return value;
        }
        
        if ("date".equals(type)) {
            return doDate(value);
        }
        
        if ("image".equals(type)) {
            return doBinary(exp);
        }
        
        if ("snippet".equals(type)) {
            return doSnippet(exp);
        }
        
        throw new RuntimeException("Unknown type: " + type);
    }
    
    private String doBinary(String value) {
        try (InputStream is = scenarioDefinition.getResourceAsStream(value)) {
            return Base64.encodeBase64String(IOUtils.toByteArray(is));
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private String doSnippet(String value) {
        value = addExtension(value, "json");
        
        try (InputStream is = scenarioDefinition.getResourceAsStream(value)) {
            return IOUtils.toString(is);
        } catch (Exception e) {
            throw MiscUtil.toUnchecked(e);
        }
    }
    
    private String doDate(String value) {
        boolean dateOnly = value.toLowerCase().trim().charAt(0) == 't';
        Date date = DateUtil.parseDate(value);
        
        if (date != null) {
            BaseDateTimeType dtt = dateOnly ? new DateType(date) : new DateTimeType(date);
            value = dtt.getValueAsString();
        } else {
            throw new RuntimeException("Bad date specification: " + value);
        }
        
        return value;
    }
}