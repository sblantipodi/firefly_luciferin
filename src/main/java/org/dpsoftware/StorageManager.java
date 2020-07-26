/*
  StorageManager.java

  Copyright (C) 2020  Davide Perini

  Permission is hereby granted, free of charge, to any person obtaining a copy of
  this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.

  You should have received a copy of the MIT License along with this program.
  If not, see <https://opensource.org/licenses/MIT/>.
*/
package org.dpsoftware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 * Write and read yaml configuration file
 */
public class StorageManager {

    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private ObjectMapper mapper;
    private String path;
    private final String settingsFileName = "FireflyLuciferin.yaml";

    /**
     * Constructor
     */
    public StorageManager() {

        // Initialize yaml file writer
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create FireflyLuciferin in the Documents folder
        path = System.getProperty("user.home") + File.separator + "Documents";
        path += File.separator + "FireflyLuciferin";
        File customDir = new File(path);

        if (customDir.exists()) {
            logger.info(customDir + " already exists");
        } else if (customDir.mkdirs()) {
            logger.info(customDir + " was created");
        } else {
            logger.info(customDir + " was not created");
        }

    }

    /**
     * Write params inside the configuration file
     * @param config file
     * @throws IOException can't write to file
     */
    public void writeConfig(Configuration config) throws IOException {

        Configuration currentConfig = readConfig();
        if (currentConfig != null) {
            File file = new File(path + File.separator + settingsFileName);
            if (file.delete()) {
                logger.info("Cleaning old config");
            } else{
                logger.info("Failed to clean old config");
            }
        }
        mapper.writeValue(new File(path + File.separator + settingsFileName), config);

    }

    /**
     * Load configuration file
     * @return config file
     */
    public Configuration readConfig() {

        Configuration config = null;
        try {
            config = mapper.readValue(new File(path + File.separator + settingsFileName), Configuration.class);
            logger.info("Configuration OK.");
        } catch (IOException e) {
            logger.error("Error reading config file, writing a default one.");
        }
        return config;

    }

}