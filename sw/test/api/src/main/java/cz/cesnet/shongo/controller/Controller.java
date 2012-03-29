package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.XmlRpcTypeConverterFactory;
import cz.cesnet.shongo.common.XmlRpcWebServer;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;

import java.io.IOException;

/**
 * Controller class
 *
 * @author Martin Srom
 */
public class Controller
{
    /**
     * Server port
     */
    public static final int port = 8008;

    /**
     * Main controller method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        try {
            PropertyHandlerMapping propertyHandlerMapping = new PropertyHandlerMapping();
            propertyHandlerMapping.setTypeConverterFactory(new XmlRpcTypeConverterFactory());
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), "xmlrpc.properties");

            XmlRpcWebServer webServer = new XmlRpcWebServer(port);
            XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
            xmlRpcServer.setHandlerMapping(propertyHandlerMapping);

            XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl)xmlRpcServer.getConfig();

            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }
}
