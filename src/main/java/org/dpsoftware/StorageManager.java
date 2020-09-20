/*
  StorageManager.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020  Davide Perini

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 * Write and read yaml configuration file
 */
public class StorageManager {

    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final ObjectMapper mapper;
    private String path;

    /**
     * Constructor
     */
    public StorageManager() {

        // Initialize yaml file writer
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Create FireflyLuciferin in the Documents folder
        path = System.getProperty(Constants.HOME_PATH) + File.separator + Constants.DOCUMENTS_FOLDER;
        path += File.separator + Constants.LUCIFERIN_FOLDER;
        File customDir = new File(path);

        if (customDir.exists()) {
            logger.info(customDir + " " + Constants.ALREADY_EXIST);
        } else if (customDir.mkdirs()) {
            logger.info(customDir + " " + Constants.WAS_CREATED);
        } else {
            logger.info(customDir + " " + Constants.WAS_NOT_CREATED);
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
            File file = new File(path + File.separator + Constants.CONFIG_FILENAME);
            if (file.delete()) {
                logger.info(Constants.CLEANING_OLD_CONFIG);
            } else{
                logger.info(Constants.FAILED_TO_CLEAN_CONFIG);
            }
        }
        mapper.writeValue(new File(path + File.separator + Constants.CONFIG_FILENAME), config);

    }

    /**
     * Load configuration file
     * @return config file
     */
    public Configuration readConfig() {

        Configuration config = null;
        try {
            config = mapper.readValue(new File(path + File.separator + Constants.CONFIG_FILENAME), Configuration.class);
            logger.info(Constants.CONFIG_OK);
        } catch (IOException e) {
            logger.error(Constants.ERROR_READING_CONFIG);
        }
        return config;

    }

}