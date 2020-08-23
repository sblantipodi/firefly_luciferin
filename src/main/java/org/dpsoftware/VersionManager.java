package org.dpsoftware;

import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dpsoftware.gui.GUIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

@Getter
@NoArgsConstructor
public class VersionManager {

    private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);

    public String version = "1.2.0";

    public boolean checkForUpgrade() {
        String urlStr = "https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/pom.xml";
        try {
            int numericVerion = Integer.parseInt(version.replace(".", ""));
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("<project.version>")) {
                    int latestRelease = Integer.parseInt(inputLine.replace("<project.version>", "")
                            .replace("</project.version>", "").replace(".","").trim());
                    if (numericVerion < latestRelease) {
                        return true;
                    }
                }
            }
            in.close();
        } catch (IOException e) {
            logger.error(e.toString());
        }
        return false;
    }

}
