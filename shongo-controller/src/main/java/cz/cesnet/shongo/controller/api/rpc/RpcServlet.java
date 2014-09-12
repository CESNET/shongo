
package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.rpc.TypeConverterFactory;
import cz.cesnet.shongo.api.rpc.TypeFactory;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.AbstractRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.report.*;
import cz.cesnet.shongo.util.Timer;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcNotAuthorizedException;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Server for XML-RPC with improved type factory.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcServlet extends XmlRpcServlet
{
    private static Logger logger = LoggerFactory.getLogger(RpcServlet.class);

    /**
     * Handler mapping, provide set of service instances
     * that will handler all XML-RPC requests.
     */
    private PropertyHandlerMapping handlerMapping;

    /**
     * Constructor.
     */
    public RpcServlet()
    {
        handlerMapping = new RpcHandlerMapping();
        handlerMapping.setTypeConverterFactory(new TypeConverterFactory());
        handlerMapping.setRequestProcessorFactoryFactory(new RpcRequestProcessorFactory());
        handlerMapping.setVoidMethodEnabled(true);
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
        // Add instance to request processor factory
        RpcRequestProcessorFactory factory =
                (RpcRequestProcessorFactory) handlerMapping.getRequestProcessorFactoryFactory();
        factory.addInstance(handler);
    }

    @Override
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException
    {
        return handlerMapping;
    }

    @Override
    protected XmlRpcServletServer newXmlRpcServer(ServletConfig pConfig) throws XmlRpcException
    {
        XmlRpcServletServer server = new XmlRpcServletServer();
        server.setTypeFactory(new TypeFactory(server));
        return server;
    }

    private static class XmlRpcServletServer extends org.apache.xmlrpc.webserver.XmlRpcServletServer
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
                try {
                    super.writeResponse(pConfig, pStream, pResult);
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
                super.writeResponse(pConfig, pStream, pResult);
            }
        }

        @Override
        protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream,
                Throwable pError) throws XmlRpcException
        {
            if (RpcServerRequestLogger.isEnabled()) {
                pStream = RpcServerRequestLogger.logResponse(pStream);
            }
            super.writeError(pConfig, pStream, pError);
        }

        @Override
        protected void setResponseHeader(ServerStreamConnection pConnection, String pHeader, String pValue)
        {
            ((Connection) pConnection).setResponseHeader(pHeader, pValue);
        }

        @Override
        protected void logError(Throwable t)
        {
        }
    }
}
