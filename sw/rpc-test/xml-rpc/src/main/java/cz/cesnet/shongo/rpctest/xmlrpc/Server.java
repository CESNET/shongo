package cz.cesnet.shongo.rpctest.xmlrpc;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.WebServer;

import java.io.IOException;

public class Server
{
    private static Logger logger = Logger.getLogger(Server.class);

    public static final int port = 8008;

    public static void main(String[] args)
    {
        logger.info("Starting XmlRpc Server...");
        try {
            PropertyHandlerMapping propertyHandlerMapping = new PropertyHandlerMapping();
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), "ServerXmlRpc.properties");

            WebServer webServer = new WebServer(port);
            XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
            xmlRpcServer.setHandlerMapping(propertyHandlerMapping);

            XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
            serverConfig.setEnabledForExtensions(true);
            serverConfig.setEnabledForExceptions(true);

            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

}
