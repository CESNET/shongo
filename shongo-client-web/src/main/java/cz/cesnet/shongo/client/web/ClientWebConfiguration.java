package cz.cesnet.shongo.client.web;

import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

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
     * Controller url.
     */
    private URL controllerUrl;

    /**
     * Application name by language code.
     */
    private Map<String, String> nameByLanguage;

    /**
     * @return {@link #clientWebConfiguration}
     */
    public static ClientWebConfiguration getInstance()
    {
        if (clientWebConfiguration == null) {
            clientWebConfiguration = new ClientWebConfiguration("shongo-client-web.cfg.xml");
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
     * @param language
     * @return application name for given {@code language}
     */
    public String getName(String language)
    {
        if (nameByLanguage == null) {
            // Load names by languages
            nameByLanguage = new HashMap<String, String>();
            for (HierarchicalConfiguration nameConfiguration : configurationsAt("name")) {
                nameConfiguration.getRoot();
                Node nameNode = nameConfiguration.getRoot();
                String nameLanguage = null;
                String nameValue = (String) nameNode.getValue();
                List<ConfigurationNode> attributeNodes = nameNode.getAttributes("language");
                if (attributeNodes.size() > 0) {
                    ConfigurationNode attributeNode = attributeNodes.get(0);
                    nameLanguage = (String) attributeNode.getValue();
                }
                if (!nameByLanguage.containsKey(nameLanguage)) {
                    nameByLanguage.put(nameLanguage, nameValue);
                }
            }
        }
        String name = nameByLanguage.get(language);
        if (name == null) {
            name = nameByLanguage.get(null);
        }
        return name;
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
     * @return {@link #controllerUrl}
     */
    public URL getControllerUrl()
    {
        if (controllerUrl == null) {
            String controllerUrl = getString("controller");
            if (!controllerUrl.startsWith("http")) {
                controllerUrl = "http://" + controllerUrl;
            }
            try {
                this.controllerUrl = new URL(controllerUrl);
                int port = this.controllerUrl.getPort();
                if (port == -1) {
                    port = 8181;
                }
                this.controllerUrl = new URL(
                        this.controllerUrl.getProtocol(), this.controllerUrl.getHost(), port, "");
            }
            catch (MalformedURLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return controllerUrl;
    }

    /**
     * @return contact email to developers
     */
    public String getSuggestionEmail()
    {
        return getString("suggestion-email");
    }

    /**
     * @return administrator emails
     */
    public Collection<String> getAdministratorEmails()
    {
        Collection<String> administrators = new HashSet<String>();
        for (Object administratorValue : getList("administrator")) {
            if (administratorValue instanceof String) {
                String administrator = (String) administratorValue;
                administrator = administrator.trim();
                if (administrator.isEmpty()) {
                    continue;
                }
                administrators.add(administrator);
            }
        }
        return administrators;
    }

    /**
     * @return administrator emails
     */
    public Collection<String> getHotlines()
    {
        Collection<String> administrators = new HashSet<String>();
        for (Object administratorValue : getList("hotline")) {
            if (administratorValue instanceof String) {
                String administrator = (String) administratorValue;
                administrator = administrator.trim();
                if (administrator.isEmpty()) {
                    continue;
                }
                administrators.add(administrator);
            }
        }
        return administrators;
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
    public String getAuthenticationClientSecret()
    {
        return getString("security.client-secret");
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

    /**
     * @return SMTP username
     */
    public String getSmtpUserName()
    {
        return getString("smtp.username");
    }

    /**
     * @return SMTP password
     */
    public String getSmtpPassword()
    {
        return getString("smtp.password");
    }
}
