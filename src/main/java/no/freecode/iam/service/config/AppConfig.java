package no.obos.iam.service.config;

import no.obos.iam.service.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Helper methods for reading configurration.
 */
public final class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    public static final String IAM_CONFIG_KEY = "IAM_CONFIG";
    public static final String IAM_MODE_KEY = "IAM_MODE";
    public static final String IAM_MODE_JUNIT = "JUNIT";
    public static final String IAM_MODE_DEV = "DEV";

    public static final AppConfig appConfig = new AppConfig();

    private final Properties properties;

    private AppConfig() {
        String configfilename = System.getProperty(IAM_CONFIG_KEY);
        if(configfilename != null) {
            properties = loadFromFile(configfilename);
        } else  {
            String appMode = System.getenv(IAM_MODE_KEY);
            if(appMode == null) {
                appMode = System.getProperty(IAM_MODE_KEY);
            }
            if(appMode == null) {
                throw new ConfigurationException("Neither " + IAM_CONFIG_KEY + " or " + IAM_MODE_KEY + " defined in environment or as system parameter");
            }
            properties = loadFromClasspath(appMode);

        }

    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    private Properties loadFromClasspath(String appMode) {
        Properties properties = new Properties();
        String propertyfile = String.format("useridentitybackend.%s.properties", appMode);
        logger.info("Loading properties from classpath: {}", propertyfile);
        InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(propertyfile);
        if(is == null) {
            throw new ConfigurationException("Error reading " + propertyfile + " from classpath.");
        }
        try {
            properties.load(is);
        } catch (IOException e) {
            throw new ConfigurationException("Error reading " + propertyfile + " from classpath.", e);
        }
        return properties;
    }

    private Properties loadFromFile(String configfilename)  {
        Properties fileProperties = new Properties();
        File file = new File(configfilename);
        FileInputStream fis = null;
        try {
            if(file.exists()) {
                fis = new FileInputStream(file);
                fileProperties.load(fis);
            } else {
                throw new ConfigurationException("Config file " + configfilename + " specified by System property " + IAM_CONFIG_KEY + " not found.");
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Config file " + configfilename + " specified by System property " + IAM_CONFIG_KEY + " not found.", e);
        } catch (IOException e) {
            throw new ConfigurationException("Config file " + configfilename + " specified by System property " + IAM_CONFIG_KEY + " not found.", e);
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("Error closing stream", e);
                }
            }
        }
        return fileProperties;
    }

}
