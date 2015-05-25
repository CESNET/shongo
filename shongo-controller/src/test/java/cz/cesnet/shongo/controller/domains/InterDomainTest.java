package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import org.junit.Test;

/**
 * Tests for Inter Domain Protocol.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class InterDomainTest extends AbstractControllerTest {
    @Override
    public void before() throws Exception {
        System.setProperty(ControllerConfiguration.INTERDOMAIN_HOST, "127.0.0.1");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_PORT, "8443");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_FORCE_HTTPS, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_CLIENT_AUTH, "false");
        System.setProperty(ControllerConfiguration.INTERDOMAIN_COMMAND_TIMEOUT, "PT10S");
        super.before();
    }

    @Test
    public void test()
    {
        ControllerConfiguration configuration = getConfiguration();
    }
}
