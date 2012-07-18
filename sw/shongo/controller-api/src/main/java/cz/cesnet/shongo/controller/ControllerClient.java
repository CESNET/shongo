package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.ComplexType;
import cz.cesnet.shongo.controller.api.FaultException;
import cz.cesnet.shongo.controller.api.xmlrpc.TypeConverterFactory;
import cz.cesnet.shongo.controller.api.xmlrpc.TypeFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.*;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

/**
 * Client for a domain controller from Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerClient
{
    /**
     * XML-RPC client.
     */
    XmlRpcClient client;

    /**
     * XML-RPC client factory for creating services.
     */
    ClientFactory clientFactory;

    /**
     * Constructor.
     */
    public ControllerClient()
    {
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public ControllerClient(String host, int port) throws Exception
    {
        connect(host, port);
    }

    /**
     * Connect to a domain controller.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public void connect(String host, int port) throws Exception
    {
        // Start client
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(String.format("http://%s:%d", host, port)));
        client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactory(client, ComplexType.Options.CLIENT));
        client.setTransportFactory(new TransportFactory(client));

        // Connect to reservation service
        clientFactory = new ClientFactory(client, new TypeConverterFactory(ComplexType.Options.CLIENT));
    }

    /**
     * @param serviceClass
     * @return service from a domain controller for the given class.
     */
    public <T> T getService(Class<T> serviceClass)
    {
        return (T) clientFactory.newInstance(serviceClass);
    }

    /**
     * Execute request on a domain controller.
     *
     * @param method
     * @param params
     * @return result
     * @throws XmlRpcException
     */
    public Object execute(String method, Object[] params) throws XmlRpcException
    {
        return client.execute(method, params);
    }

    /**
     * Execute request on a domain controller.
     *
     * @param method
     * @param params
     * @return result
     * @throws XmlRpcException
     */
    public Object execute(String method, List params) throws XmlRpcException
    {
        return client.execute(method, params);
    }

    /**
     * Transport factory which throws from {@link XmlRpcStreamTransport#readResponse(XmlRpcStreamRequestConfig,
     * java.io.InputStream)} {@link FaultException} instead of {@link XmlRpcException}.
     */
    private static class TransportFactory extends XmlRpcSun15HttpTransportFactory {
        /**
         * Private attribute in {@link XmlRpcSun15HttpTransportFactory}.
         */
        private Proxy proxy;

        /**
         * Constructor.
         *
         * @param client
         */
        public TransportFactory(XmlRpcClient client)
        {
            super(client);
        }

        @Override
        public void setProxy(Proxy pProxy) {
            proxy = pProxy;
        }

        @Override
        public XmlRpcTransport getTransport() {
            XmlRpcSun15HttpTransport transport = new XmlRpcSun15HttpTransport(getClient()){
                @Override
                protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream)
                        throws XmlRpcException
                {
                    try {
                        return super.readResponse(pConfig, pStream);
                    } catch (XmlRpcException exception) {
                        throw new FaultException(exception.code, exception.getMessage());
                    }
                }
            };
            transport.setSSLSocketFactory(getSSLSocketFactory());
            transport.setProxy(proxy);
            return transport;
        }
    }
}
