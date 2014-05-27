package cz.cesnet.shongo.connector.support;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

/**
 * Represents an improved {@link XmlRpcTransportFactory} which allows for keep-alive connection.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class KeepAliveTransportFactory extends XmlRpcCommonsTransportFactory
{
    /**
     * Single {@link XmlRpcTransport}.
     */
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
            transport = new Transport(this);
        }
        return transport;
    }

    public static class Transport extends XmlRpcCommonsTransport
    {
        public Transport(XmlRpcCommonsTransportFactory pFactory)
        {
            super(pFactory);
        }

        @Override
        protected void initHttpHeaders(XmlRpcRequest pRequest) throws XmlRpcClientException
        {
            super.initHttpHeaders(pRequest);
            setRequestHeader("Connection", "Keep-Alive");
        }
    }
}