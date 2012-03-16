package cz.cesnet.shongo.rpctest.xmlrpc;

import cz.cesnet.shongo.rpctest.common.API;
import cz.cesnet.shongo.rpctest.common.XmlFormatter;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.*;
import org.xml.sax.SAXException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

public class Client
{
    private final static Logger logger = Logger.getLogger(Client.class);

    public static void main(String[] args)
    {
        try {
            XmlRpcClientConfigImpl xmlRpcClientConfig = new XmlRpcClientConfigImpl();
            xmlRpcClientConfig.setServerURL(new URL("http://127.0.0.1:" + Server.port + "/rpctest"));

            XmlRpcClient xmlRpcClient = new XmlRpcClient();
            xmlRpcClient.setConfig(xmlRpcClientConfig);
            xmlRpcClient.setTransportFactory(new TransportFactory(xmlRpcClient));
            xmlRpcClient.setTypeFactory(new TypeFactory(xmlRpcClient));

            xmlRpcClient.execute("API.getResource", new Object[]{});
            
            System.out.println(xmlRpcClient.execute("API.formatDate", new Object[]{new API.Date("xx")}));
            System.out.println(xmlRpcClient.execute("API.formatDate", new Object[]{new API.PeriodicDate("xx", "yy")}));

            Object[] params = new Object[]{new Integer(33), new Integer(5)};
            Integer result = (Integer) xmlRpcClient.execute("API.div", params);
            System.out.println(result);

            String message = (String) xmlRpcClient.execute("API.getMessage", new Object[]{});

        } catch (XmlRpcException exception) {
            exception.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * XmlRpc Transport Factory that log request and response XML content
     */
    public static class TransportFactory extends XmlRpcSun15HttpTransportFactory {

        public TransportFactory(XmlRpcClient client) {
            super(client);
        }

        @Override
        public XmlRpcTransport getTransport() {
            return new LoggingTransport(this);
        }

        private class LoggingTransport extends XmlRpcSunHttpTransport {

            public LoggingTransport(TransportFactory pFactory) {
                super(pFactory.getClient());
            }

            @Override
            protected void writeRequest(final ReqWriter writer) throws XmlRpcException, IOException, SAXException {
                super.writeRequest(writer);
                final StringBuilder builder = new StringBuilder();
                writer.write(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        builder.append((char) b );
                    }
                });
                logger.info("REQUEST \n" + XmlFormatter.format(builder.toString()));
            }

            @Override
            protected InputStream getInputStream() throws XmlRpcException {
                InputStream stream = super.getInputStream();

                StringBuilder builder = new StringBuilder();
                try {
                    stream.mark(999999);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                    }
                    stream.reset();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.info("RESPONSE \n" + XmlFormatter.format(builder.toString()));
                return stream;
            }
        }
    }
}
