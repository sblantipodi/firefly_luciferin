package org.dpsoftware.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.InstanceConfigurer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class StorageManagerCubeLutTest {
    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({
            "1, , FireflyLuciferin.yaml",
            "2, '', FireflyLuciferin_2.yaml",
            "3, tray.icon.default, FireflyLuciferin_3.yaml",
            "2, Cinema, 2_Cinema.yaml"
    })
    void changesOnlyStoredLutInActiveConfiguration(int instance, String profile, String filename) throws Exception {
        MainSingleton main = new MainSingleton();
        main.whoAmI = instance;
        main.profileArg = profile;
        main.config = new Configuration();
        main.config.setBrightness(99);
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        Path configFile = directory.resolve(filename);
        Files.writeString(configFile, "cubeLut: old.cube\nbrightness: 37\nunknownSetting:\n  values: [keep, 42]\n");
        ObjectNode expected = (ObjectNode) yaml.readTree(configFile.toFile());
        expected.put("cubeLut", Constants.DISABLED);
        Path other = directory.resolve("other-profile.yaml");
        Files.writeString(other, "cubeLut: untouched.cube\n");

        try (var paths = mockStatic(InstanceConfigurer.class);
             var singleton = mockStatic(MainSingleton.class)) {
            paths.when(InstanceConfigurer::getConfigPath).thenReturn(directory.toString());
            singleton.when(MainSingleton::getInstance).thenReturn(main);
            new StorageManager().writeCubeLut(Constants.DISABLED);
        }

        assertEquals(expected, yaml.readTree(configFile.toFile()));
        assertEquals("cubeLut: untouched.cube\n", Files.readString(other));
        assertEquals(99, main.config.getBrightness());
    }
}
