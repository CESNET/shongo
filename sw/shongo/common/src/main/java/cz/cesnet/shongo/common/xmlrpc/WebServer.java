package cz.cesnet.shongo.common.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * XmlRpc WebServer with improved type factory
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class WebServer extends org.apache.xmlrpc.webserver.WebServer
{
    private static Logger logger = LoggerFactory.getLogger(WebServer.class);

    /**
     * Handler mapping, provide set of service instances
     * that will handler all XML-RPC requests.
     */
    private PropertyHandlerMapping handlerMapping;

    /**
     * Get host address by name
     *
     * @param host
     * @return host address
     */
    public static java.net.InetAddress getHostByName(String host)
    {
        if (host != null) {
            try {
                return InetAddress.getByName(host);
            }
            catch (UnknownHostException e) {
            }

        }
        return null;
    }

    /**
     * Construct XML-RPC web server
     *
     * @param pPort
     */
    public WebServer(String host, int pPort)
    {
        super(pPort, getHostByName(host));

        handlerMapping = new PropertyHandlerMapping();
        handlerMapping.setTypeConverterFactory(new TypeConverterFactory());
        handlerMapping.setRequestProcessorFactoryFactory(new RequestProcessorFactory());

        XmlRpcServer xmlRpcServer = getXmlRpcServer();
        xmlRpcServer.setHandlerMapping(handlerMapping);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
    }

    @Override
    protected XmlRpcStreamServer newXmlRpcStreamServer()
    {
        XmlRpcStreamServer server = new ConnectionServer();
        server.setTypeFactory(new TypeFactory(server));
        return server;
    }

    /**
     * Set handlers from property file.
     * Each line in file should contain pair "handler_name=handler_class".
     *
     * @param mappingFile
     */
    public void setHandlerFromFile(String mappingFile)
    {
        try {
            handlerMapping.load(Thread.currentThread().getContextClassLoader(), mappingFile);
        }
        catch (Exception exception) {
            logger.error("Failed to load handler mappings from file.", exception);
        }
    }

    /**
     * Add handler by instance
     *
     * @param name
     * @param handler
     */
    public void addHandler(String name, Object handler)
    {
        try {
            handlerMapping.addHandler(name, handler.getClass());
        }
        catch (XmlRpcException exception) {
            logger.error("Failed to add new handler.", exception);
        }
        // Add instance to request processory factory
        RequestProcessorFactory factory = (RequestProcessorFactory) handlerMapping.getRequestProcessorFactoryFactory();
        factory.addInstance(handler);
    }

    /**
     * Stop XML-RPC server
     */
    public void stop()
    {
        shutdown();
    }

    /**
     * We need to override request processor factory which creates instances of handler objects.
     * Default implementation create new instance for each request which is not desireable for
     * our purposes and this class overrides this mechanism to return existing instances
     * for specified class.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class RequestProcessorFactory
            extends RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory
    {
        private Map<Class, Object> instances = new HashMap<Class, Object>();

        public void addInstance(Object object)
        {
            instances.put(object.getClass(), object);
        }

        @Override
        protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest) throws XmlRpcException
        {
            if (instances.containsKey(pClass)) {
                return instances.get(pClass);
            }
            return super.getRequestProcessor(pClass, pRequest);
        }
    }

    /**
     * Connection server. Copied default implementation which only overrides
     * convertThrowable method to allow use of cause from runtime exception
     * as fault.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ConnectionServer extends XmlRpcHttpServer
    {
        @Override
        protected Throwable convertThrowable(Throwable pError)
        {
            if (pError instanceof RuntimeException) {
                return pError.getCause();
            }
            return pError;
        }

        @Override
        protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream,
                Throwable pError) throws XmlRpcException
        {
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeError(pConfig, pStream, pError);
                    data.getConnection().writeError(data, pError, (ByteArrayOutputStream) pStream);
                }
                else {
                    data.getConnection().writeErrorHeader(data, pError, -1);
                    super.writeError(pConfig, pStream, pError);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void writeResponse(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Object pResult)
                throws XmlRpcException
        {
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeResponse(pConfig, pStream, pResult);
                    data.getConnection().writeResponse(data, pStream);
                }
                else {
                    data.getConnection().writeResponseHeader(data, -1);
                    super.writeResponse(pConfig, pStream, pResult);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void setResponseHeader(ServerStreamConnection pConnection, String pHeader, String pValue)
        {
            ((Connection) pConnection).setResponseHeader(pHeader, pValue);
        }
    }
}
