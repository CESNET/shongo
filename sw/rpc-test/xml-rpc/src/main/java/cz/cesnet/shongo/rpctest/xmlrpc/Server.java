package cz.cesnet.shongo.rpctest.xmlrpc;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.TypeParser;
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
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), "ServerXmlRpc.properties");

            WebServer webServer = new CustomWebServer(port);
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

    private static class CustomTypeFactory extends TypeFactoryImpl {
        public CustomTypeFactory(XmlRpcController pController) {
            super(pController);
        }

        public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }

        public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
            TypeSerializer serializer = super.getSerializer(pConfig, pObject);
            if ( serializer == null ) {
                serializer = new MapSerializer(this, pConfig) {
                    @Override
                    public void write(ContentHandler pHandler, Object pObject) throws SAXException {
                        Map map = null;
                        try {
                            map = BeanUtils.describe(pObject);
                            map.remove("class");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        super.write(pHandler, map);
                    }
                };
            }
                System.out.println("OK");
                System.out.println(serializer.toString());
            return serializer;
        }
    }

    public static class CustomWebServer extends WebServer {
        public CustomWebServer(int pPort) {
            super(pPort);
        }

        public CustomWebServer(int pPort, InetAddress pAddr) {
            super(pPort, pAddr);
        }

        protected XmlRpcStreamServer newXmlRpcStreamServer() {
            XmlRpcStreamServer server = super.newXmlRpcStreamServer();
            server.setTypeFactory(new CustomTypeFactory(server));
            return server;
        }
    }

}
