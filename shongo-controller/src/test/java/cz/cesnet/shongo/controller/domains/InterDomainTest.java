package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.junit.Test;

/**
 * Tests for Inter Domain Protocol.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainTest extends AbstractControllerTest {
    private static final String INTERDOMAIN_LOCAL_HOST = "localhost";
    private static final String INTERDOMAIN_LOCAL_PORT = "8443";
    private static final String INTERDOMAIN_LOCAL_PASSWORD = "shongo_test";
    private static final String INTERDOMAIN_LOCAL_PASSWORD_HASH = SSLCommunication.hashPassword(INTERDOMAIN_LOCAL_PASSWORD.getBytes());

    @Override
    public void before() throws Exception {
        System.setProperty(ControllerConfiguration.INTERDOMAIN_HOST, INTERDOMAIN_LOCAL_HOST);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PORT, INTERDOMAIN_LOCAL_PORT);
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE, "shongo-controller/src/test/resources/keystore/server.p12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_PASSWORD, "shongo");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_SSL_KEY_STORE_TYPE, "PKCS12");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PKI_CLIENT_AUTH, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT, "PT10S");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_BASIC_AUTH_PASSWORD, INTERDOMAIN_LOCAL_PASSWORD);
        super.before();
    }

    @Test
    public void testBasicAuthLogin()
    {
        Domain loopbackDomain = new Domain();
        loopbackDomain.setOrganization("CESNET z.s.p.o.");
        loopbackDomain.setName("TEST");
        loopbackDomain.setCode(LocalDomain.getLocalDomainCode());
        loopbackDomain.setAllocatable(true);
        loopbackDomain.setCertificatePath("shongo-controller/src/test/resources/keystore/server.crt");
        DeviceAddress deviceAddress = new DeviceAddress(INTERDOMAIN_LOCAL_HOST, Integer.parseInt(INTERDOMAIN_LOCAL_PORT));
        loopbackDomain.setDomainAddress(deviceAddress);
        loopbackDomain.setPasswordHash(INTERDOMAIN_LOCAL_PASSWORD_HASH);
        getResourceService().createDomain(SECURITY_TOKEN_ROOT, loopbackDomain);

        InterDomainAgent.getInstance().login(loopbackDomain);
    }

    @Test
    public void test() throws Exception {

    }


}
