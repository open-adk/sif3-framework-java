/*
 * BearerSecurityFactory.java
 * Created: 15/12/2014
 *
 * Copyright 2014 Systemic Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License 
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package sif3.common.security;

import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import au.com.systemic.framework.utils.AdvancedProperties;
import au.com.systemic.framework.utils.StringUtils;

/**
 * This class is the factory for the security service. The SIF3 Framework supports Basic and SIF_HMACSHA256 as security and authentication
 * mechanism. These are defined as the known security concepts for SIF 3.0.1. If any other more generic security shall be used, such
 * as OAuth, Active Directroy etc instead of the SIF 3.0.1 specified options then this framework allows this by means of implementing a
 * custom security interface. This factory class gives access to this custom security interface. The SIF3 Framework will always use this
 * custom security implementation if the HTTP Authorization header carries a 'Bearer' token rather than a 'Basic' or 'SIF_HMACSHA256' token.<br/><br/>
 * 
 * To enable to 'Bearer' token security implementation the lower level of the SIF3 Framework will use this BearerSecurityFactory to get a
 * specific implementation to such a custom security service. The actual custom implementation to be used is defined in the provider's or
 * consumer's property file. Please refer to these property files for more details.<br/><br/>
 * 
 * <b>Note</b><br/>
 * As of January 2015 SIF 3.0.1 does not officially specifiy any other security mechanism except 'Basic' and 'SIF_HMACSH256'. If a SIF 3
 * implementation using this framework chooses to use a custom security service such as OAuth and therefore implements such a service and
 * links it with this framework, then this is done at the developper's own risk that it may not integrate with other SIF 3 systems/services. 
 * 
 * @author Joerg Huber
 *
 */
public class BearerSecurityFactory
{
  private static final Logger logger = Logger.getLogger(BearerSecurityFactory.class);

  private static final String SECURITY_SERVICE_PROP_NAME = "adapter.security.service";
  
  private static BearerSecurityFactory factory = null;
  
  private AdvancedProperties properties = null;

  /* Constructor of the actual security service as per properties file */
  private Constructor<?> constructor = null;
  
  /**
   * Returns the concrete implementation of a security service as defined in the consumer's or provider's property file. If no such
   * service is defined to the instantaiation of the security service failed then an error is logged and null is returned.
   * 
   * @param properties The consumer's or provider's property file content.
   * 
   * @return See desc.
   */
  public static AbstractSecurityService getSecurityService(AdvancedProperties properties)
  {
    try
    {
      if (factory == null) // not yet inistailised. First time access.
      {
        factory = new BearerSecurityFactory(properties);
      }
     
      // If we get here then we do have a factory initialised and we should be able to instantiate the security service.
      // Instantiate the class. Constructor must exist otherwise an exception would have been called before.
      if (factory.getConstructor() != null)
      {
        Object classObj = factory.getConstructor().newInstance(new Object[] { properties });
      
        // Check if the class extends AbstractSecurityService
        if (classObj instanceof AbstractSecurityService)
        {
          return (AbstractSecurityService)classObj;
        }
        else
        {
          logger.error("The class "+classObj.getClass().getSimpleName()+" does not extend AbstractSecurityService. This framework cannot use this security service implementation.");
          return null;
        }
      }
      else
      {
        logger.error("No valid constructor can be found for a custom security service. See previous log entries for details.");
        return null;
      }
    } 
    catch (Exception ex)
    {
      logger.error("Failed to initialise BearerSecurityFactory factory: " + ex.getMessage(), ex);
      factory = null;
      
      // Indicate to the caller that call failed.
      return null;
    }
  }
  
  
  /*---------------------*/
  /*-- Private methods --*/
  /*---------------------*/
  private BearerSecurityFactory(AdvancedProperties properties) throws Exception
  {
    if (properties == null)
    {
      throw new IllegalArgumentException("Properties given to BearerSecurityFactory is null.");
    }
    setProperties(properties);
    String securityClassName = getProperties().getPropertyAsString(SECURITY_SERVICE_PROP_NAME, null);
    if (StringUtils.notEmpty(securityClassName)) // we found a security class. => gather info about it.
    {
      Class<?> clazz = Class.forName(securityClassName);
      setConstructor(clazz.getConstructor(new Class[] { AdvancedProperties.class }));
    }
    else
    {
      logger.info("The property '"+SECURITY_SERVICE_PROP_NAME+"' is not set in the service property file. No Security Service will be available.");
    }
  }
  
  private AdvancedProperties getProperties()
  {
    return properties;
  }

  private void setProperties(AdvancedProperties properties)
  {
    this.properties = properties;
  }
  
  private Constructor<?> getConstructor()
  {
    return constructor;
  }

  private void setConstructor(Constructor<?> constructor)
  {
    this.constructor = constructor;
  }
}
