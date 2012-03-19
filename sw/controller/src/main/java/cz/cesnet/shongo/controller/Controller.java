package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Date;
import cz.cesnet.shongo.PeriodicDate;
import cz.cesnet.shongo.Type;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.*;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.apache.xmlrpc.server.XmlRpcStreamServer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import sun.reflect.generics.scope.ClassScope;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

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
            propertyHandlerMapping.setTypeConverterFactory(new TypeConverterFactory());
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), "xmlrpc.properties");

            WebServer webServer = new WebServer(port);
            XmlRpcServer xmlRpcServer = webServer.getXmlRpcServer();
            xmlRpcServer.setHandlerMapping(propertyHandlerMapping);
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

    /**
     * XmlRpc WebServer with improved type factory
     *
     * @author Martin Srom
     */
    public static class WebServer extends org.apache.xmlrpc.webserver.WebServer {
        public WebServer(int pPort) {
            super(pPort);
        }
        protected XmlRpcStreamServer newXmlRpcStreamServer() {
            XmlRpcStreamServer server = super.newXmlRpcStreamServer();
            server.setTypeFactory(new TypeFactory(server));
            return server;
        }
    }

    /**
     * TypeFactory that converts between objects and maps
     *
     * @author Martin Srom
     */
    public static class TypeFactory extends TypeFactoryImpl
    {
        public TypeFactory(XmlRpcController pController) {
            super(pController);
        }

        public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
            if ( MapSerializer.STRUCT_TAG.equals(pLocalName) ) {
                return new MapParser(pConfig, pContext, this){
                    @Override
                    public void endElement(String pURI, String pLocalName, String pQName) throws SAXException {
                        super.endElement(pURI, pLocalName, pQName);
                        try {
                            Map map = (Map)getResult();
                            if ( map != null ) {
                                String className = (String)map.get("class");
                                Class objectClass = Class.forName("cz.cesnet.shongo." + className);
                                Object object = objectClass.newInstance();
                                BeanUtils.populate(object, map);
                                setResult(object);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
            } else {
                return super.getParser(pConfig, pContext, pURI, pLocalName);
            }
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
            return serializer;
        }
    }

    /**
     * TypeConverterFactory that allows custom classes as method parameters
     *
     * @author Martin Srom
     */
    public static class TypeConverterFactory extends TypeConverterFactoryImpl
    {
        private static class IdentityTypeConverter implements TypeConverter {
            private final Class clazz;
            IdentityTypeConverter(Class pClass) {
                clazz = pClass;
            }
            public boolean isConvertable(Object pObject) {
                return pObject == null  ||  clazz.isAssignableFrom(pObject.getClass());
            }
            public Object convert(Object pObject) {
                return pObject;
            }
            public Object backConvert(Object pObject) {
                return pObject;
            }
        }

        private static final TypeConverter typeConverter = new IdentityTypeConverter(Type.class);

        @Override
        public TypeConverter getTypeConverter(Class pClass) {
            if ( Type.class.isAssignableFrom(pClass) ) {
                return typeConverter;
            }
            return super.getTypeConverter(pClass);
        }
    }
}
