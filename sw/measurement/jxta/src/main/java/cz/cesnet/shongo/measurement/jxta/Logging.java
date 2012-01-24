package cz.cesnet.shongo.measurement.jxta;

import java.net.URL;
import java.util.logging.LogManager;

/**
 * Logging util
 *
 * @author Martin Srom
 */
public class Logging {
    public static void reconfigure() {
        String file = System.getProperty("java.util.logging.config.file");
        if ( file == null || file.isEmpty() ) {
            URL url = null;
            try {
                url = Peer.class.getClassLoader().getResource("logging.properties");
                if (url == null) {
                    System.err.println("Cannot find logging.properties.");
                } else {
                    LogManager.getLogManager().readConfiguration(url.openStream());
                }
            } catch (Exception e) {
                System.err.println("Error reading logging.properties from '" + url + "': " + e);
            }
        }
    }
}
