package cz.cesnet.shongo;

import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.ServletWebServer;
import org.apache.xmlrpc.webserver.WebServer;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcServer
{
    private static final int port = 9090;

    public static class Calculator
    {
        public int add(int i1, int i2)
        {
            return i1 + i2;
        }

        public int subtract(int i1, int i2)
        {
            return i1 - i2;
        }
    }

    public static class Conference
    {
        public Map status(Map map)
        {
            return new HashMap();
        }
    }

    public static void main(String[] args) throws Exception
    {
        WebServer webServer = new WebServer(port);

        org.apache.xmlrpc.server.XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();

        PropertyHandlerMapping phm = new PropertyHandlerMapping();
        phm.addHandler("Calculator", Calculator.class);
        phm.addHandler("conference", Conference.class);
        xmlRpcServer.setHandlerMapping(phm);

        XmlRpcServerConfigImpl serverConfig =
                (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);
        serverConfig.setKeepAliveEnabled(true);

        webServer.start();
    }
}
