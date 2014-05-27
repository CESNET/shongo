package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.support.KeepAliveTransportFactory;
import org.apache.xmlrpc.client.*;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Tests for using the XML-RPC client with {@link cz.cesnet.shongo.connector.support.KeepAliveTransportFactory}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcClientTest
{
    private static Logger logger = LoggerFactory.getLogger(XmlRpcClientTest.class);

    private WebServer webServer;

    @Before
    public void before() throws Exception
    {
        webServer = new WebServer(8888);
        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler("test", TestService.class);

        xmlRpcServer.setHandlerMapping(phm);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        serverConfig.setContentLengthOptional(false);
        serverConfig.setKeepAliveEnabled(true);

        webServer.start();
    }

    @Test
    public void test() throws Exception
    {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();

        config.setServerURL(new URL("http", "127.0.0.1", 8888, "/test"));
        XmlRpcClient client = new XmlRpcClient();
        client.setTransportFactory(new KeepAliveTransportFactory(client));
        client.setConfig(config);
        Object[] params = new Object[]{};
        for (int index = 0; index < 3; index++) {
            client.execute("test.test", params);
            Thread.sleep(100);
        }
        Thread.sleep(200);
    }

    public static class TestService
    {
        public String test()
        {
            logger.debug("OK");
            return "OK";
        }
    }
}
