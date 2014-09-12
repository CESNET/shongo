
package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.rpc.TypeConverterFactory;
import cz.cesnet.shongo.api.rpc.TypeFactory;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.report.*;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Server for XML-RPC with improved type factory.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcServer extends org.apache.xmlrpc.webserver.WebServer
{
    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);

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
     * @param host
     * @param pPort
     */
    public RpcServer(String host, int pPort)
    {
        super(pPort, getHostByName(host));

        handlerMapping = new RpcHandlerMapping();
        handlerMapping.setTypeConverterFactory(new TypeConverterFactory());
        handlerMapping.setRequestProcessorFactoryFactory(new RpcRequestProcessorFactory());
        handlerMapping.setVoidMethodEnabled(true);

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
            throw new RuntimeException("Failed to load handler mappings from file.", exception);
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
            throw new RuntimeException("Failed to add new handler.", exception);
        }
        // Add instance to request processory factory
        RpcRequestProcessorFactory factory = (RpcRequestProcessorFactory) handlerMapping.getRequestProcessorFactoryFactory();
        factory.addInstance(handler);
    }

    @Override
    public void log(Throwable pError)
    {
        if (pError instanceof SocketException) {
            if (serverSocket == null) {
                return;
            }
        }
        super.log(pError);
    }

    /**
     * Stop XML-RPC server
     */
    public void stop()
    {
        shutdown();
        try {
            ServerSocket serverSocket = this.serverSocket;
            this.serverSocket = null;
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    /**
     * Connection server. Copied default implementation which overrides:
     * 1) convertThrowable method to allow use of cause from runtime exception
     * as fault.
     * 2) getRequest and writeResponse for logging of XML-RPC requests and response XMLs.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ConnectionServer extends XmlRpcHttpServer
    {
        @Override
        protected Throwable convertThrowable(Throwable throwable)
        {
            return RpcHandler.convertThrowable(throwable);
        }

        @Override
        protected XmlRpcRequest getRequest(XmlRpcStreamRequestConfig pConfig, InputStream pStream)
                throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logRequest(pStream);
                try {
                    return super.getRequest(pConfig, pStream);
                }
                finally {
                    try {
                        pStream.close();
                    }
                    catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            else {
                return super.getRequest(pConfig, pStream);
            }
        }

        @Override
        protected void writeResponse(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Object pResult)
                throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logResponse(pStream);
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
                catch (IOException exception) {
                    throw new XmlRpcException(exception.getMessage(), exception);
                }
                finally {
                    try {
                        pStream.close();
                    }
                    catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            else {
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
                catch (IOException exception) {
                    throw new XmlRpcException(exception.getMessage(), exception);
                }
            }
        }

        @Override
        protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream,
                Throwable pError) throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logResponse(pStream);
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
                catch (IOException exception) {
                    throw new XmlRpcException(exception.getMessage(), exception);
                }
                finally {
                    try {
                        pStream.close();
                    }
                    catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            else {
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
                catch (IOException exception) {
                    throw new XmlRpcException(exception.getMessage(), exception);
                }
            }
        }

        @Override
        protected void setResponseHeader(ServerStreamConnection pConnection, String pHeader, String pValue)
        {
            ((Connection) pConnection).setResponseHeader(pHeader, pValue);
        }

        @Override
        public void execute(XmlRpcStreamRequestConfig pConfig, ServerStreamConnection pConnection)
                throws XmlRpcException
        {
            super.execute(pConfig, pConnection);
        }

        @Override
        protected void logError(Throwable t)
        {
        }
    }
}
