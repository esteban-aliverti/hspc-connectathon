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

import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.cwf.fhir.common.FhirUtil;

import ca.uhn.fhir.model.api.Tag;

public class DemoUtils {
    
    
    public static final String DEMO_URN = "urn:cogmedsys:hsp:model:demo";
    
    /**
     * Identifier used to locate demo resources for bulk deletes.
     */
    public static final Tag DEMO_GROUP_TAG = new Tag(DEMO_URN, "gen", "Demo Data");
    
    /**
     * Convenience method for creating identifiers in local system.
     * 
     * @param system The identifier system.
     * @param value The identifier value.
     * @return The newly created identifier.
     */
    public static Identifier createIdentifier(String system, Object value) {
        Identifier identifier = new Identifier();
        identifier.setSystem("urn:cogmedsys:hsp:model:" + system);
        identifier.setValue(value.toString());
        return identifier;
    }
    
    /**
     * Convenience method for creating identifiers for resources belonging to a patient. The
     * identifier generated will be unique across all resources.
     * 
     * @param system The identifier system.
     * @param idnum The identifier value.
     * @param patient Owner of the resource to receive the identifier.
     * @return The newly created identifier.
     */
    public static Identifier createIdentifier(String system, int idnum, Patient patient) {
        String value = getMainIdentifier(patient).getValue() + "_" + idnum;
        return createIdentifier(system, value);
    }
    
    /**
     * Convenience method to create a time offset.
     * 
     * @param minuteOffset Offset in minutes.
     * @return A date minus the offset.
     */
    public static Date createDateWithMinuteOffset(long minuteOffset) {
        return new Date(System.currentTimeMillis() - minuteOffset * 60 * 1000);
    }
    
    /**
     * Convenience method to create a time offset.
     * 
     * @param dayOffset Offset in days.
     * @return A date minus the offset.
     */
    public static Date createDateWithDayOffset(long dayOffset) {
        return createDateWithMinuteOffset(dayOffset * 24 * 60);
    }
    
    /**
     * Convenience method to create a time offset.
     * 
     * @param yearOffset Offset in years.
     * @return A date minus the offset.
     */
    public static Date createDateWithYearOffset(long yearOffset) {
        return createDateWithDayOffset(yearOffset * 365);
    }
    
    /**
     * Returns a random element from a string array.
     * 
     * @param choices The array of possible choices.
     * @return A random element.
     */
    public static String getRandom(String[] choices) {
        int index = (int) (Math.random() * choices.length);
        return choices[index];
    }
    
    /**
     * Returns the principal identifier for the given resource.
     * 
     * @param resource The resource whose main identifier is sought.
     * @return The main identifier, or null if not found.
     */
    public static Identifier getMainIdentifier(DomainResource resource) {
        List<Identifier> identifiers = FhirUtil.getIdentifiers(resource);
        return FhirUtil.getFirst(identifiers);
    }
    
    /**
     * Adds the demo tag to a resource for bulk deletes.
     * 
     * @param resource The resource.
     */
    public static void addDemoTag(IBaseResource resource) {
        FhirUtil.addTag(DEMO_GROUP_TAG, resource);
    }
    
    /**
     * Adds the demo tag to a resource for scenario-based deletes.
     * 
     * @param resource The resource.
     * @param scenario The scenario to which this resource belongs.
     * @param label The tag label.
     */
    public static void addDemoTag(IBaseResource resource, String scenario, String label) {
        FhirUtil.addTag(createScenarioTag(scenario, label), resource);
    }
    
    /**
     * Creates a tag to be used for scenario-based deletes.
     * 
     * @param scenario The scenario name.
     * @param label The tag's label.
     * @return The newly created tag.
     */
    public static Tag createScenarioTag(String scenario, String label) {
        return new Tag(DEMO_URN, scenario, label);
    }
    
    public static Tag getScenarioTag(IBaseResource resource, String scenario) {
        IBaseCoding coding = resource.getMeta().getTag(DEMO_URN, scenario);
        return coding == null ? null : new Tag(coding.getSystem(), coding.getCode(), coding.getDisplay());
    }
    
}