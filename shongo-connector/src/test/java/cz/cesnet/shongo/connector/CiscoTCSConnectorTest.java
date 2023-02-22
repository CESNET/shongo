package cz.cesnet.shongo.connector;


import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.common.ConnectorConfigurationImpl;
import cz.cesnet.shongo.connector.device.CiscoMCUConnector;
import org.junit.Assert;
import org.apache.log4j.Level;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;
import org.junit.Test;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link cz.cesnet.shongo.connector.device.CiscoTCSConnector}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CiscoTCSConnectorTest
{
    private static boolean invokeConnectionReset = false;

    @Test
    public void testConnectionReset() throws Exception
    {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XmlRpcErrorLogger.class);
        if (logger != null) {
            logger.setLevel(Level.OFF);
        }

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
        connector.connect(new ConnectorConfigurationImpl(
                DeviceAddress.parseAddress("http://127.0.0.1:" + webServer.getPort()), "test", "test"));
        invokeConnectionReset = true;
        Assert.assertNotNull(connector.listRooms());
        Assert.assertFalse(invokeConnectionReset);
    }

    public static class McuRpcDevice
    {
        public Map query(Map parameters)
        {
            Map<String, String> result = new HashMap<String, String>();
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
            Map<String, String> result = new HashMap<String, String>();
            return result;
        }
    }
}
