package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification;
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

    public DomainAuthentication(EntityManagerFactory entityManagerFactory, ControllerConfiguration configuration, EmailSender emailSender) {
        domainsAccessTokens.setExpiration(Duration.standardDays(1));
        this.configuration = configuration;
        domainService = new DomainService(entityManagerFactory);
        domainService.init(configuration);
        this.notifier = new DomainAdminNotifier(emailSender, configuration);
        try {
            KeyStore keyStore = KeyStore.getInstance(configuration.getInterDomainSslKeyStoreType());
            FileInputStream keyStoreFile = new FileInputStream(configuration.getInterDomainSslKeyStore());
            keyStore.load(keyStoreFile, configuration.getInterDomainSslKeyStorePassword().toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, configuration.getInterDomainSslKeyStorePassword().toCharArray());
            this.keyManagerFactory = keyManagerFactory;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Failed to load keystore " + configuration.getInterDomainSslKeyStore(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read keystore " + configuration.getInterDomainSslKeyStore(), e);
        }
    }

    /**
     * Returns
     * @return
     */
    protected Map<X509Certificate, Domain> listForeignDomainCertificates() {
        Map<X509Certificate, Domain> domainsByCert = new HashMap<X509Certificate, Domain>();
        for (Domain domain : domainService.listDomains()) {
            String certificate = domain.getCertificatePath();
            if (Strings.isNullOrEmpty(certificate)) {
                //exclude local domain
                if (domain.getStatus() != null) {
                    continue;
                }
                if (configuration.requiresClientPKIAuth()) {
                    String message = "Cannot connect to domain " + domain.getName()
                            + ", certificate file does not exist or is not configured.";
                    logger.error(message);
                    notifier.notifyDomainAdmin(message, null);
                }
                continue;
            }
            try {
                domainsByCert.put(SSLCommunication.readPEMCert(certificate), domain);
            } catch (CertificateException e) {
                String message = "Failed to load certificate file " + certificate;
                logger.error(message, e);
                notifier.notifyDomainAdmin(message, e);
            } catch (IOException e) {
                String message = "Cannot read certificate file " + certificate;
                logger.error(message, e);
                notifier.notifyDomainAdmin(message, e);
            }
        }
        return domainsByCert;
    }

    /**
     * Generates access token for foreign domain and store it.
     * @param domain
     * @return
     */
    protected synchronized String generateAccessToken(Domain domain) {
        String accessToken = null;
        while (accessToken == null) {
            String generatedToken = RandomStringUtils.random(8, true, true) + DateTime.now().getMillis() + RandomStringUtils.random(8, true, true);
            if (!domainsAccessTokens.contains(generatedToken)) {
                accessToken = generatedToken;
            }
        }
        domainsAccessTokens.put(accessToken, domain.getCode());
        return accessToken;
    }

    protected Domain getDomain(String accessToken) {
        domainsAccessTokens.clearExpired(DateTime.now());
        String domainCode = domainsAccessTokens.get(accessToken);
        return (domainCode == null ? null : domainService.findDomainByCode(domainCode));
    }

    protected Domain getDomain(X509Certificate certificate) {
        return listForeignDomainCertificates().get(certificate);
    }

    protected KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

//    public AuthType getAuthType() {
//        //TODO
//        return null;
//    }
//
//    public static enum AuthType {
//        BASIC,
//        CLIENT_CERT;
//    }
}
