package cz.cesnet.shongo.rpctest.xmlrpc;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.*;
import org.apache.xmlrpc.parser.DateParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.DateSerializer;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.WebServer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Map;

public class Server
{
    private static Logger logger = Logger.getLogger(Server.class);

    public static final int port = 8008;

    public static void main(String[] args)
    {
        logger.info("Starting XmlRpc Server...");
        try {
            PropertyHandlerMapping propertyHandlerMapping = new PropertyHandlerMapping();
            propertyHandlerMapping.setTypeConverterFactory(new TypeConverterFactory());
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), "ServerXmlRpc.properties");

            WebServer webServer = new CustomWebServer(port);
            XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
            xmlRpcServer.setHandlerMapping(propertyHandlerMapping);

            XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
            //serverConfig.setEnabledForExtensions(true);
            //serverConfig.setEnabledForExceptions(true);

            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

    public static class CustomWebServer extends WebServer {
        public CustomWebServer(int pPort) {
            super(pPort);
        }
        protected XmlRpcStreamServer newXmlRpcStreamServer() {
            XmlRpcStreamServer server = super.newXmlRpcStreamServer();
            server.setTypeFactory(new TypeFactory(server));
            return server;
        }
    }

}
