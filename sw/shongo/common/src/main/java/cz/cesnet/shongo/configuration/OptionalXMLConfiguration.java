package cz.cesnet.shongo.configuration;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;

/**
 * {@link XMLConfiguration} which doesn't throw {@link ConfigurationException} when the file doesn't exists.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OptionalXMLConfiguration extends XMLConfiguration
{
    /**
     * Creates a new instance of{@code XMLConfiguration}. If the specified file exists the
     * configuration is loaded from it.
     *
     * @param fileName the name of the file to load
     */
    public OptionalXMLConfiguration(String fileName)
    {
        super();
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        try {
            load(file);
        }
        catch (ConfigurationException exception) {
            throw new RuntimeException("Configuration cannot be loaded from file " + fileName + ".", exception);
        }
    }
}
