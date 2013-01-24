package cz.cesnet.shongo.api.xmlrpc;

import org.apache.xmlrpc.client.*;

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
