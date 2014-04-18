package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.util.Address;
import junit.framework.Assert;
import org.apache.log4j.Level;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.junit.Test;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link cz.cesnet.shongo.connector.CiscoTCSConnector}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CiscoMCUConnectorTest
{
    private static boolean invokeConnectionReset = false;

    @Test
    public void test() throws Exception
    {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XmlRpcErrorLogger.class);
        logger.setLevel(Level.OFF);

        WebServer webServer = new WebServer(0);
        XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
        PropertyHandlerMapping handlerMapping = new PropertyHandlerMapping();
        handlerMapping.addHandler("device", McuRpcDevice.class);
        handlerMapping.addHandler("conference", McuRpcConference.class);
        xmlRpcServer.setHandlerMapping(handlerMapping);
        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);
        webServer.start();

        CiscoMCUConnector connector = new CiscoMCUConnector(){
            @Override
            protected boolean isExecApiRetryPossible(XmlRpcException exception)
            {
                return exception.getMessage().endsWith("Connection reset") || super.isExecApiRetryPossible(exception);
            }
        };
        connector.connect(Address.parseAddress("http://127.0.0.1:" + webServer.getPort()), "test", "test");
        invokeConnectionReset = true;
        connector.getRoomList();
        Assert.assertFalse(invokeConnectionReset);
    }

    public static class McuRpcDevice
    {
        public Map query(Map parameters)
        {
            Map result = new HashMap();
            result.put("apiVersion", "2.9");
            result.put("model", "Codian MCU Test");
            return result;
        }
    }

    public static class McuRpcConference
    {
        public Map enumerate(Map parameters) throws Exception
        {
            if (invokeConnectionReset) {
                invokeConnectionReset = false;
                throw new SocketException("Connection reset");
            }
            Map result = new HashMap();
            return result;
        }
    }
}
