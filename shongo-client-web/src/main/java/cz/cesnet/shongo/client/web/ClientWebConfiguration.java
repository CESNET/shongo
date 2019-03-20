package cz.cesnet.shongo.client.web;

import com.google.common.base.Strings;
import net.tanesha.recaptcha.ReCaptcha;
import org.apache.commons.configuration.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Web client configuration.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWebConfiguration extends CombinedConfiguration {
    /**
     * Single instance of {@link ClientWebConfiguration}.
     */
    private static ClientWebConfiguration clientWebConfiguration;

    /**
     * Base path for default design folder.
     */
    private String defaultDesignFolderBasePath = "";

    /**
     * Controller url.
     */
    private URL controllerUrl;

    /**
     * @return {@link #clientWebConfiguration}
     */
    public static ClientWebConfiguration getInstance() {
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
    private ClientWebConfiguration(String adminConfigFileName) {
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
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load administrator configuration.", exception);
        }
        // Default configuration has the lowest priority
        try {
            addConfiguration(new XMLConfiguration("client-web-default.cfg.xml"));
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load default configuration.", exception);
        }
    }

    /**
     * @param defaultDesignFolderBasePath sets the {@link #defaultDesignFolderBasePath}
     */
    public void setDefaultDesignFolderBasePath(String defaultDesignFolderBasePath) {
        this.defaultDesignFolderBasePath = defaultDesignFolderBasePath;
    }

    /**
     * @return server port
     */
    public int getServerPort() {
        return getInt("server.port");
    }

    /**
     * @return server port
     */
    public int getServerSslPort() {
        return getInt("server.ssl-port");
    }

    /**
     * @return ssl key store
     */
    public String getServerSslKeyStore() {
        String sslKeyStore = getString("server.ssl-key-store");
        if (sslKeyStore == null || sslKeyStore.trim().isEmpty()) {
            return null;
        }
        return sslKeyStore;
    }

    /**
     * @return password for ssl key store
     */
    public String getServerSslKeyStorePassword() {
        return getString("server.ssl-key-store-password");
    }

    /**
     * @return server context path
     */
    public String getServerPath() {
        return getString("server.path");
    }

    /**
     * @return true whether requests should be redirected to HTTPS
     */
    public boolean isServerForceHttps() {
        return getBoolean("server.force-https");
    }

    /**
     * @return true whether requests are forwarded
     */
    public boolean isServerForwarded() {
        return getBoolean("server.forwarded");
    }

    /**
     * @return forwarded host
     */
    public String getServerForwardedHost() {
        String forwardedHost = getString("server.forwarded-host");
        if (forwardedHost == null || forwardedHost.trim().isEmpty()) {
            return null;
        }
        return forwardedHost;
    }

    /**
     * @return {@link #controllerUrl}
     */
    public URL getControllerUrl() {
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
            } catch (MalformedURLException exception) {
                throw new RuntimeException(exception);
            }
        }
        return controllerUrl;
    }

    /**
     * @return administrator emails
     */
    public Collection<String> getAdministratorEmails() {
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
    public Collection<String> getHotlines() {
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
    public String getAuthenticationServerUrl() {
        return getString("security.server") + "/oidc-authn/oic";
    }

    /**
     * @return authentication client id
     */
    public String getAuthenticationClientId() {
        return getString("security.client-id");
    }

    /**
     * @return authentication client redirect url
     */
    public String getAuthenticationRedirectUri() {
        return getString("security.redirect-uri");
    }

    /**
     * @return authentication client secret
     */
    public String getAuthenticationClientSecret() {
        return getString("security.client-secret");
    }

    /**
     * @return SMTP sender email address
     */
    public String getSmtpSender() {
        return getString("smtp.sender");
    }

    /**
     * @return SMTP server host
     */
    public String getSmtpHost() {
        return getString("smtp.host");
    }

    /**
     * @return SMTP server port
     */
    public String getSmtpPort() {
        return getString("smtp.port");
    }

    /**
     * @return subject prefix for all email messages
     */
    public String getSmtpSubjectPrefix() {
        return getString("smtp.subject-prefix");
    }

    /**
     * @return SMTP username
     */
    public String getSmtpUserName() {
        return getString("smtp.username");
    }

    /**
     * @return SMTP password
     */
    public String getSmtpPassword() {
        return getString("smtp.password");
    }

    /**
     * @return name of tag for meeting rooms
     */
    public String getMeetingRoomTagName() {
        return getString("tags.meeting-room");
    }

    /**
     * @return name of tag for meeting rooms
     */
    public Boolean showOnlyMeetingRooms() {
        try {
            return getBoolean("design.show-only-meeting-rooms");
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    /**
     * @return url to folder with design files (not ending with "/")
     */
    public String getDesignFolder() {
        String design = getString("design.folder");
        if (design == null || design.equals("(default)")) {
            design = defaultDesignFolderBasePath + "/WEB-INF/default-design";
        } else {
            design = "file:" + design;
        }
        while (design.endsWith("/")) {
            design = design.substring(0, design.length() - 1);
        }
        return design;
    }

    public boolean isOffline() {
        return getBoolean("testing.offline", false);
    }

    public String getOfflineRootAccessToken() {
        return getString("testing.root-access-token", "UNDEFINED");
    }

    /**
     * @return {@link HierarchicalConfiguration} of design parameters
     */
    public HierarchicalConfiguration getDesignParameters() {
        return configurationAt("design.parameters");
    }

    /**
     * @return public key for {@link ReCaptcha}
     */
    public String getReCaptchaPublicKey() {
        return getString("recaptcha.public-key");
    }

    /**
     * @return private key for {@link ReCaptcha}
     */
    public String getReCaptchaPrivateKey() {
        return getString("recaptcha.private-key");
    }

    public String getE164Pattern() {
        return getString("virtual-room-settings.h323-e164-number");
    }

    /**
     * @return true whether {@link ReCaptcha} is properly configured, false otherwise
     */
    public boolean isReCaptchaConfigured() {
        return !Strings.isNullOrEmpty(getReCaptchaPublicKey()) && !Strings.isNullOrEmpty(getReCaptchaPrivateKey());
    }
}
