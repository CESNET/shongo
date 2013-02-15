package cz.cesnet.shongo.connector;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.client.XmlRpcTransportFactory;

/**
 * Represents an improved {@link XmlRpcTransportFactory} which allows for keep-alive connection.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class KeepAliveTransportFactory extends XmlRpcSun15HttpTransportFactory
{
    private XmlRpcTransport transport;

    /**
     * Creates a new instance.
     *
     * @param pClient The client, which is controlling the factory.
     */
    public KeepAliveTransportFactory(XmlRpcClient pClient)
    {
        super(pClient);
    }

    @Override
    public XmlRpcTransport getTransport()
    {
        if (transport == null) {
            transport = super.getTransport();
        }
        return transport;
    }
}
