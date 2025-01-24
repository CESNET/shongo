package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.persistence.EntityManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Domains authentication for
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainAuthentication {
    private final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    private final DomainService domainService;

    private final ControllerConfiguration configuration;

    private final DomainAdminNotifier notifier;

    private final KeyManagerFactory keyManagerFactory;

    private final ExpirationMap<String, String> domainsAccessTokens = new ExpirationMap<>();

    public DomainAuthentication(ControllerConfiguration configuration,
                                DomainService domainService, DomainAdminNotifier notifier) {
        domainsAccessTokens.setExpiration(Duration.standardDays(1));
        this.configuration = configuration;
        this.domainService = domainService;
        this.notifier = notifier;
        try {
            KeyStore keyStore = KeyStore.getInstance(configuration.getRESTApiSslKeyStoreType());
            FileInputStream keyStoreFile = new FileInputStream(configuration.getRESTApiSslKeyStore());
            keyStore.load(keyStoreFile, configuration.getRESTApiSslKeyStorePassword().toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, configuration.getRESTApiSslKeyStorePassword().toCharArray());
            this.keyManagerFactory = keyManagerFactory;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to load keystore " + configuration.getRESTApiSslKeyStore(), e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read keystore " + configuration.getRESTApiSslKeyStore(), e);
        }
    }

    /**
     * Returns
     * @return
     */
    synchronized protected Map<X509Certificate, Domain> listForeignDomainCertificates() {
        Map<X509Certificate, Domain> domainsByCert = new HashMap<>();
        for (Domain domain : domainService.listForeignDomains()) {
            String certificate = domain.getCertificatePath();
            if (Strings.isNullOrEmpty(certificate)) {
                if (configuration.requiresClientPKIAuth()) {
                    String message = "Cannot connect to domain " + domain.getName()
                            + ", certificate file does not exist or is not configured.";
                    logger.error(message);
                    notifier.notifyDomainAdmins(message, null);
                }
                continue;
            }
            try {
                domainsByCert.put(SSLCommunication.readPEMCert(certificate), domain);
            } catch (CertificateException e) {
                String message = "Failed to load certificate file " + certificate;
                logger.error(message, e);
                notifier.notifyDomainAdmins(message, e);
            } catch (IOException e) {
                String message = "Cannot read certificate file " + certificate;
                logger.error(message, e);
                notifier.notifyDomainAdmins(message, e);
            }
        }
        return domainsByCert;
    }

    /**
     * Generates access token for foreign domain and store it.
     * @param domain
     * @return
     */
    protected String generateAccessToken(Domain domain) {
        String accessToken = null;
        synchronized (domainsAccessTokens) {
            while (accessToken == null) {
                String generatedToken = RandomStringUtils.random(8, true, true) + DateTime.now().getMillis() + RandomStringUtils.random(8, true, true);
                if (!domainsAccessTokens.contains(generatedToken)) {
                    accessToken = generatedToken;
                }
            }
            domainsAccessTokens.put(accessToken, domain.getName());
        }
        return accessToken;
    }

    protected Domain getDomain(String accessToken) {
        synchronized (domainsAccessTokens) {
            domainsAccessTokens.clearExpired(DateTime.now());
            String domainName = domainsAccessTokens.get(accessToken);
            return (domainName == null ? null : domainService.findDomainByName(domainName));
        }
    }

    protected Domain getDomain(X509Certificate certificate) {
        return listForeignDomainCertificates().get(certificate);
    }

    synchronized protected KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }
}
