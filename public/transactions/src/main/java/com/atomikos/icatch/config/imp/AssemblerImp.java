package com.atomikos.icatch.config.imp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import com.atomikos.icatch.config.Assembler;
import com.atomikos.icatch.config.ConfigProperties;
import com.atomikos.logging.LoggerFactory;
import com.atomikos.util.ClassLoadingHelper;

public class AssemblerImp implements Assembler {
	
	private static final String DEFAULT_PROPERTIES_FILE_NAME = "transactions-defaults.properties";

	private static final String JTA_PROPERTIES_FILE_NAME = "jta.properties";

	private static final String TRANSACTIONS_PROPERTIES_FILE_NAME = "transactions.properties";
	
	private static com.atomikos.logging.Logger LOGGER = LoggerFactory.createLogger(AssemblerImp.class);
	
    private void loadPropertiesFromClasspath(Properties p, String fileName){
    		URL url = null;
    		
    		//first look in application classpath (cf ISSUE 10091)
    		url = ClassLoadingHelper.loadResourceFromClasspath(getClass(), fileName);		
    		if (url == null) {
    			url = getClass().getClassLoader().getSystemResource ( fileName );
    		}
    		if (url != null) {
    			InputStream in;
				try {
					in = url.openStream();
					p.load(in);
					in.close();
				} catch (IOException e) {
					LOGGER.logWarning("Failed to load property file: " + fileName, e);
				}
    		} else {
    			LOGGER.logWarning("Could not find expected property file: " + fileName);
    		}
    }

	/**
	 * Called by ServiceLoader.
	 */
	public AssemblerImp() {
	}

	@Override
	public ConfigProperties getConfigProperties() {
		Properties defaults = new Properties();
		loadPropertiesFromClasspath(defaults, DEFAULT_PROPERTIES_FILE_NAME);
		Properties transactionsProperties = new Properties(defaults);
		loadPropertiesFromClasspath(transactionsProperties, TRANSACTIONS_PROPERTIES_FILE_NAME);
		Properties jtaProperties = new Properties(transactionsProperties);
		loadPropertiesFromClasspath(jtaProperties, JTA_PROPERTIES_FILE_NAME);
		Properties finalProperties = new Properties(jtaProperties);
		applySystemProperties(finalProperties);
		return new ConfigProperties(finalProperties);
	}

	private void applySystemProperties(Properties finalProperties) {
		Properties systemProperties = System.getProperties();
		Enumeration<?> propertyNames = systemProperties.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String name = (String) propertyNames.nextElement();
			if (name.startsWith("com.atomikos")) {
				finalProperties.setProperty(name, systemProperties.getProperty(name));
			}
		}
	}
}
