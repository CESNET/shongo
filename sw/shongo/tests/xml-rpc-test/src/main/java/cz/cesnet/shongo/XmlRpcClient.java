package cz.cesnet.shongo;

import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSun15HttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;

import java.net.URL;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcClient
{
    public static class KeepAliveTransportFactory extends XmlRpcSun15HttpTransportFactory
    {
        private XmlRpcTransport transport;

        /**
         * Creates a new instance.
         *
         * @param pClient The client, which is controlling the factory.
         */
        public KeepAliveTransportFactory(org.apache.xmlrpc.client.XmlRpcClient pClient)
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

    public static void main(String[] args) throws Exception {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL("http://127.0.0.1:9090"));
        org.apache.xmlrpc.client.XmlRpcClient client = new org.apache.xmlrpc.client.XmlRpcClient();
        client.setTransportFactory(new KeepAliveTransportFactory(client));
        client.setConfig(config);
        for (int i = 0; i < 2; i++) {
            Object[] params = new Object[]{new Integer(33), new Integer(9)};
            Integer result = (Integer) client.execute("Calculator.add", params);
            System.out.println("Result: " + result.toString());

            Thread.sleep(1000);
        }
    }
}
