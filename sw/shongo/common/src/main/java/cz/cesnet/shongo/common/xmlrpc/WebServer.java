package cz.cesnet.shongo.common.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.ServerStreamConnection;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * XmlRpc WebServer with improved type factory
 *
 * @author Martin Srom
 */
public class WebServer extends org.apache.xmlrpc.webserver.WebServer
{
    public WebServer(int pPort, String mappingFile)
    {
        super(pPort);

        PropertyHandlerMapping propertyHandlerMapping = new PropertyHandlerMapping();
        propertyHandlerMapping.setTypeConverterFactory(new TypeConverterFactory());
        try {
            propertyHandlerMapping.load(Thread.currentThread().getContextClassLoader(), mappingFile);
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }

        XmlRpcServer xmlRpcServer = getXmlRpcServer();
        xmlRpcServer.setHandlerMapping(propertyHandlerMapping);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
    }

    protected XmlRpcStreamServer newXmlRpcStreamServer()
    {
        XmlRpcStreamServer server = new ConnectionServer();
        server.setTypeFactory(new TypeFactory(server));
        return server;
    }

    /**
     * Connection server
     *
     * @author Martin Srom
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
