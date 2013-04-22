package cz.cesnet.shongo;

import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.*;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.HashMap;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcClient
{
    //private static final String URL = "http://127.0.0.1:9090";
    //private static final String URL = "https://mcuc.cesnet.cz/RPC2";
    private static final String URL = "https://mcu-1.sukb.muni.cz/RPC2";
    private static final String PASSWORD = "";

    public static void main(String[] args) throws Exception
    {
        initSsl();

        org.apache.xmlrpc.client.XmlRpcClient client = createClient(URL);
        for (int i = 0; i < 2; i++) {
            try {
                System.out.println("Sending Request...");
                Object result = sendRequest(client);
                //Object result = sendRequest(url);
                System.out.print("Result: ");
                System.out.println(result);
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }

            Thread.sleep(6000);
        }
    }

    public static org.apache.xmlrpc.client.XmlRpcClient createClient(String url) throws Exception
    {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url));

        org.apache.xmlrpc.client.XmlRpcClient client = new org.apache.xmlrpc.client.XmlRpcClient();
        client.setTransportFactory(new KeepAliveTransportFactory(client));
        client.setConfig(config);
        return client;
    }

    public static URL createConnection(String url) throws Exception
    {
        return new URL(url);
    }

    public static Object sendRequest(org.apache.xmlrpc.client.XmlRpcClient client) throws Exception
    {
        Object[] params = new Object[]{new HashMap<String, String>()
        {{
                put("authenticationUser", "shongo");
                put("authenticationPassword", PASSWORD);
                put("conferenceName", "shongo-test");
            }}
        };
        return client.execute("conference.status", params);
    }

    public static Object sendRequest(String url) throws Exception
    {
        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.connect();

        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        wr.write(
                "<?xml version=\"1.0\"?>\n" +
                "<methodCall>\n" +
                "   <methodName>conference.status</methodName>\n" +
                "   <params>\n" +
                "       <param><value><struct>\n" +
                "           <member>\n" +
                "               <name>authenticationUser</name>\n" +
                "               <value><string>shongo</string></value>\n" +
                "           </member>\n" +
                "           <member>\n" +
                "               <name>authenticationPassword</name>\n" +
                "               <value><string>" + PASSWORD + "</string></value>\n" +
                "           </member>\n" +
                "           <member>\n" +
                "               <name>conferenceName</name>\n" +
                "               <value><string>shongo-test</string></value>\n" +
                "           </member>\n" +
                "       </struct></value></param>\n" +
                "   </params>\n" +
                "</methodCall>");
        wr.flush();
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
            result.append("\n");
        }
        wr.close();
        rd.close();
        return result.toString();
    }

    private static void initSsl() throws Exception
    {
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager()
        {
            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType)
            {
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType)
            {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }
        }
        };
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        SSLContext.setDefault(sslContext);
    }

    public static class KeepAliveTransportFactory extends XmlRpcCommonsTransportFactory
    {
        private XmlRpcTransport transport;

        public KeepAliveTransportFactory(org.apache.xmlrpc.client.XmlRpcClient pClient)
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
}
