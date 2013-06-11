package cz.cesnet.shongo.client.web;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.File;

/**
 * Web client configuration.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebConfiguration extends CombinedConfiguration
{
    /**
     * Constructor.
     *
     * @param adminConfigFileName filename of administrator configuration file
     */
    public ClientWebConfiguration(String adminConfigFileName)
    {
        // System properties has the highest priority
        addConfiguration(new SystemConfiguration());
        // Administrator configuration has lower priority
        try {
            File adminConfigFile = new File(adminConfigFileName);
            if (adminConfigFile.exists()) {
                addConfiguration(new XMLConfiguration(adminConfigFile));
            }
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load administrator configuration.", exception);
        }
        // Default configuration has the lowest priority
        try {
            addConfiguration(new XMLConfiguration("default.cfg.xml"));
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default configuration.", exception);
        }
    }

    /**
     * @return controller url
     */
    public String getControllerUrl()
    {
        return getString("controller");
    }

    /**
     * @return title suffix
     */
    public String getTitleSuffix()
    {
        return getString("title-suffix");
    }

    /**
     * @return contact email to developers
     */
    public String getContactEmail()
    {
        return getString("contact");
    }

    /**
     * @return administrator email
     */
    public String getAdministratorEmail()
    {
        return getString("administrator");
    }

    /**
     * @return url of authentication server
     */
    public String getAuthenticationServerUrl()
    {
        return getString("security.server") + "/authn/oic";
    }

    /**
     * @return authentication client id
     */
    public String getAuthenticationClientId()
    {
        return getString("security.client-id");
    }

    /**
     * @return authentication client redirect url
     */
    public String getAuthenticationRedirectUri()
    {
        return getString("security.redirect-uri");
    }

    /**
     * @return authentication client secret
     */
    public String getAuthenticationSecret()
    {
        return getString("security.secret");
    }
}
