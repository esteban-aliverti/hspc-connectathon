/*
 * #%L
 * Web App Demo
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
package org.hspconsortium.cwfdemo.setup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carewebframework.common.StrUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.Resource;

public class DemoSetup implements BeanPostProcessor {
    
    private static final Log log = LogFactory.getLog(DemoSetup.class);
    
    public DemoSetup(String connectionUrl, String username, String password, Resource sqlResource) throws Exception {
        
        Class.forName("org.h2.Driver");
        
        try (Connection conn = DriverManager.getConnection(connectionUrl, username, password);) {
            List<String> lines = IOUtils.readLines(sqlResource.getInputStream());
            PreparedStatement ps = conn.prepareStatement(StrUtil.fromList(lines));
            ps.execute();
        } catch (Exception e) {
            log.error("Error during demo setup.  This can occur if setup has already been processed.\n\n" + e.getMessage());
        }
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
}
