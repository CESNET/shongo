package cz.cesnet.shongo.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

/**
 * Makes HTTPS connection.
 * <ul>
 *     <li>supports multiple client certificates</li>
 *     <li>supports own set of root CAs</li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>
 */
public class SSLComunication {
    private static final Logger log = LoggerFactory.getLogger(SSLComunication.class);

    //TODO======================SMAZAT===============================
//
//    private static final String PASSWORD = "ShoT42paIn";
//    private static final List<String> CA_CERT_FILES = Arrays.asList(
//            "/home/opicak/shongo/shongo-deployment/keystore/hd3.fi.muni.cz.cert"
////            "/etc/grid-security/certificates/3c58f906.0"
////            , "/etc/ssl/certs/Verisign_Class_3_Public_Primary_Certification_Authority.pem"
//    );
//
//    public static void main(String[] args) throws MalformedURLException {
//        URL url = new URL("https://hd3.fi.muni.cz:8443/rest/sec");
//        try {
//            HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
//            setUpSSLConnection(uc);
//            int status = uc.getResponseCode();
//            log.debug("SSL ciphersuite: {}", uc.getCipherSuite());
//            log.info("HTTP Status " + status);
//            for (int i = 1; true; i++) {
//                String httpHeaderName = uc.getHeaderFieldKey(i);
//                if (httpHeaderName == null) break;
//                System.out.println(httpHeaderName + ": " + uc.getHeaderField(i));
//            }
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//        }
//    }
//
//    private static void setUpSSLConnection(HttpsURLConnection uc) {
//        try {
//            MyKeyManager myKeyManager = new MyKeyManager();
//            myKeyManager.addKeyAndCert(readKeyStore("/home/opicak/shongo/shongo-deployment/keystore/server.keystore", "l5h1q78eii"));
////            myKeyManager.addKeyAndCert(readKeyStore("/home/makub/.globus/makub_TERENA_Personal_MU_2.p12", PASSWORD));
////            myKeyManager.addKeyAndCert(readKeyStore("/home/makub/.globus/makub_TERENA_Grid_MU_4.p12", PASSWORD));
////            myKeyManager.addKeyAndCert(readKeyStore("/home/makub/.globus/makub_TERENA_Personal_CESNET_2.p12", PASSWORD));
//            SSLContext sc = SSLContext.getInstance("SSL");
//            sc.init(new KeyManager[]{myKeyManager}, new TrustManager[]{new MyTrustManager(CA_CERT_FILES)}, null);
//            uc.setSSLSocketFactory(sc.getSocketFactory());
//        } catch (Exception e) {
//            throw new RuntimeException("cannot set up SSL ", e);
//        }
//    }

    //TODO======================SMAZAT===============================

    /**
     * KeyManager that stores keys and cert chains in memory.
     */
    static public class SimpleKeyManager implements X509KeyManager {

        final static Logger log = LoggerFactory.getLogger(SimpleKeyManager.class);

        private Map<String, PrivateKey> privateKeyMap = new HashMap<String, PrivateKey>();
        private Map<String, X509Certificate[]> chainMap = new LinkedHashMap<String, X509Certificate[]>();

        public void addKeyAndCert(KeyAndCert kc) {
            privateKeyMap.put(kc.getAlias(), kc.getPrivateKey());
            chainMap.put(kc.getAlias(), kc.getChain());
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] principals) {
            //patrne se nepouziva
            if (log.isDebugEnabled()) {
                log.debug("getClientAliases(keyType={})", keyType);
                for (Principal p : principals) {
                    X500Principal x500p = (X500Principal) p;
                    log.debug("DN: {}", getX500Name(x500p));
                }
            }
            return null;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] principals, Socket socket) {
            List<String> keyTypes = Arrays.asList(keyType);
            Set<Principal> principalSet = new HashSet<Principal>(Arrays.asList(principals));
            for (Map.Entry<String, X509Certificate[]> ch : chainMap.entrySet()) {
                X509Certificate[] chain = ch.getValue();
                if(principalSet.contains(chain[chain.length-1].getSubjectX500Principal())) {
                    String alias = ch.getKey();
                    PrivateKey privateKey = privateKeyMap.get(alias);
                    if(keyTypes.contains(privateKey.getAlgorithm())) {
                        log.debug("found {} private key+cert chain for {}",privateKey.getAlgorithm(),getX500Name(chain[0].getSubjectX500Principal()));
                        return alias;
                    }
                }
            }
            log.warn("no private key and cert were found matching the server requirements");
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return chainMap.get(alias);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return privateKeyMap.get(alias);
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            throw new RuntimeException("not implemented");
        }
    }

    /**
     * Trust manager that uses its own set of root CAs.
     */
    static public class CATrustManager implements X509TrustManager {

        final static Logger log = LoggerFactory.getLogger(CATrustManager.class);

        private final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        private final CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");
        private final List<TrustAnchor> rootCAs;
        private final PKIXParameters validationParameters;

        public CATrustManager(List<String> caCertFiles) throws CertificateException, IOException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
            rootCAs = buildRootCAs(caCertFiles);
            validationParameters = buildValidationParameters(rootCAs);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            throw new RuntimeException("checkClientTrusted() not implemented");
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            CertPath certPath = certificateFactory.generateCertPath(Arrays.asList(x509Certificates));
            try {
                PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult) certPathValidator.validate(certPath, validationParameters);
                log.debug("validated against CA \"{}\"", toDN(pkixResult.getTrustAnchor()));
            } catch (Exception e) {
                log.warn("server SSL cert chain not valid");
                for (int i = 0; i < x509Certificates.length; i++) {
                    X509Certificate cert = x509Certificates[i];
                    log.warn(" {} s: {}", i, getX500Name(cert.getSubjectX500Principal()));
                    log.warn("   i: {}", getX500Name(cert.getIssuerX500Principal()));
                }
                Throwable t = e;
                while (t.getCause() != null) t = t.getCause(); //get root exception
                throw new CertificateException(t.getMessage(), e);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] caCerts = new X509Certificate[rootCAs.size()];
            for (int i = 0; i < rootCAs.size(); i++) {
                caCerts[i] = rootCAs.get(i).getTrustedCert();
            }
            return caCerts;
        }

        static private PKIXParameters buildValidationParameters(List<TrustAnchor> rootCAs) throws CertificateException, IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
            PKIXParameters params = new PKIXParameters(new HashSet<TrustAnchor>(rootCAs));
            params.setRevocationEnabled(false); // Disable CRL checking since we are not supplying any CRLs
            return params;
        }

        static private List<TrustAnchor> buildRootCAs(List<String> caCertFiles) throws CertificateException, IOException {
            List<TrustAnchor> anchors = new ArrayList<TrustAnchor>();
            for (String caCert : caCertFiles) {
                TrustAnchor ca = new TrustAnchor(readPEMCert(caCert), null);
                log.debug("adding root CA \"{}\"", toDN(ca));
                anchors.add(ca);
            }
            return anchors;
        }


    }

    static private String toDN(TrustAnchor ca) {
        return getX500Name(ca.getTrustedCert().getSubjectX500Principal());
    }

    /**
     * Converts X509 DN to string with OIDs translated.
     * @param x500Principal prinicpal
     * @return RFC2253 encoded DN
     */
    static private String getX500Name(X500Principal x500Principal) {
        Map<String, String> oidMap = new HashMap<String, String>();
        oidMap.put("1.2.840.113549.1.9.2", "unstructuredName");
        oidMap.put("2.5.4.17", "postalCode");
        oidMap.put("2.5.4.5", "serialNumber");
        oidMap.put("2.5.4.15", "businessCategory");
        oidMap.put("1.3.6.1.4.1.311.60.2.1.2", "jurisdictionOfIncorporationStateOrProvinceName");
        oidMap.put("1.3.6.1.4.1.311.60.2.1.3", "jurisdictionOfIncorporationCountryName");
        return x500Principal.getName(X500Principal.RFC2253, oidMap);
    }

    /**
     * Reads X509 certificate from a PEM encoded file.
     * @param certFile file
     * @return certificate
     * @throws CertificateException
     * @throws IOException
     */
    static public X509Certificate readPEMCert(String certFile) throws CertificateException, IOException {
        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
        File cf = new File(certFile);
        byte[] b = new byte[(int) cf.length()];
        FileInputStream fis = new FileInputStream(cf);
        //noinspection ResultOfMethodCallIgnored
        fis.read(b, 0, b.length);
        String s = new String(b);
        int z1 = s.indexOf("-----BEGIN CERTIFICATE-----");
        String END = "-----END CERTIFICATE-----";
        int z2 = s.indexOf(END, z1);
        String c = s.substring(z1, z2 + END.length());
        ByteArrayInputStream bain = new ByteArrayInputStream(c.getBytes());
        return (X509Certificate) certFact.generateCertificate(bain);
    }

    /**
     * Keeps private key and certificate together.
     */
    public static class KeyAndCert {
        private String alias;
        private PrivateKey privateKey = null;
        private X509Certificate[] chain = null;

        public KeyAndCert(String alias, PrivateKey privateKey, Certificate[] chain) {
            this.alias = alias;
            this.privateKey = privateKey;
            this.chain = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++) {
                this.chain[i] = (X509Certificate) chain[i];
            }
        }

        public String getAlias() {
            return alias;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }

        public X509Certificate[] getChain() {
            return chain;
        }
    }

    /**
     * Reads a file of type JKS or PKCS12 with private key and certificate chain.
     * @param keyStorePath path to key store file
     * @param keyStoreType
     * @param password password
     * @return private key with associated certificate and its trust chain up to the root CA
     */
    static public KeyAndCert readKeyStore(String keyStorePath, String keyStoreType, String password) {
        Throwable exception = null;
        try {
            KeyStore store = KeyStore.getInstance(keyStoreType);
            store.load(new FileInputStream(keyStorePath), password.toCharArray());
            for (String alias : Collections.list(store.aliases())) {
                if (store.isKeyEntry(alias)) {
                    return new KeyAndCert(alias,(PrivateKey) store.getKey(alias, password.toCharArray()), store.getCertificateChain(alias));
                }
            }
            throw new RuntimeException("No private key found in "+ keyStorePath);
        } catch (CertificateException e) {
            exception = e;
        } catch (UnrecoverableKeyException e) {
            exception = e;
        } catch (NoSuchAlgorithmException e) {
            exception = e;
        } catch (KeyStoreException e) {
            exception = e;
        } catch (FileNotFoundException e) {
            exception = e;
        } catch (IOException e) {
            exception = e;
        } finally {
            if (exception != null) {
                log.error("Cannot read keystore", exception);
                throw new RuntimeException("Cannot read private key from "+ keyStorePath);
            }
        }
        return null;
    }
}