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
     * Single instance of {@link ClientWebConfiguration}.
     */
    private static ClientWebConfiguration clientWebConfiguration;

    /**
     * @return {@link #clientWebConfiguration}
     */
    public static ClientWebConfiguration getInstance()
    {
        if (clientWebConfiguration == null) {
            clientWebConfiguration = new ClientWebConfiguration("client-web.cfg.xml");
        }
        return clientWebConfiguration;
    }

    /**
     * Constructor.
     *
     * @param adminConfigFileName filename of administrator configuration file
     */
    private ClientWebConfiguration(String adminConfigFileName)
    {
        // System properties has the highest priority
        addConfiguration(new SystemConfiguration());
        // Administrator configuration has lower priority
        try {
            File adminConfigFile = new File(adminConfigFileName);
            if (adminConfigFile.exists()) {
                XMLConfiguration xmlConfiguration = new XMLConfiguration();
                xmlConfiguration.setDelimiterParsingDisabled(true);
                xmlConfiguration.load(adminConfigFile);
                addConfiguration(xmlConfiguration);
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
     * @return server port
     */
    public int getServerPort()
    {
        return getInt("server.port");
    }

    /**
     * @return server port
     */
    public int getServerSslPort()
    {
        return getInt("server.ssl-port");
    }

    /**
     * @return ssl key store
     */
    public String getServerSslKeyStore()
    {
        String sslKeyStore = getString("server.ssl-key-store");
        if (sslKeyStore == null || sslKeyStore.trim().isEmpty()) {
            return null;
        }
        return sslKeyStore;
    }

    /**
     * @return password for ssl key store
     */
    public String getServerSslKeyStorePassword()
    {
        return getString("server.ssl-key-store-password");
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

    /**
     * @return SMTP sender email address
     */
    public String getSmtpSender()
    {
        return getString("smtp.sender");
    }

    /**
     * @return SMTP server host
     */
    public String getSmtpHost()
    {
        return getString("smtp.host");
    }

    /**
     * @return SMTP server port
     */
    public String getSmtpPort()
    {
        return getString("smtp.port");
    }

    /**
     * @return subject prefix for all email messages
     */
    public String getSmtpSubjectPrefix()
    {
        return getString("smtp.subject-prefix");
    }
}
