package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.xmlrpc.TypeConverterFactory;
import cz.cesnet.shongo.controller.api.xmlrpc.TypeFactory;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

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
        client.setTypeFactory(new TypeFactory(client, true));

        // Connect to reservation service
        clientFactory = new ClientFactory(client, new TypeConverterFactory(true));
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
}
