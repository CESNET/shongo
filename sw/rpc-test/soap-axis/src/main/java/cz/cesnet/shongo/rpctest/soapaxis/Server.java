package cz.cesnet.shongo.rpctest.soapaxis;

import cz.cesnet.shongo.rpctest.common.API;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.transport.http.SimpleHTTPServer;


public class Server
{
    public static final int port = 8008;

    public static void main(String[] args)
    {
        try {
            ConfigurationContext context = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            AxisService service = AxisService.createService(API.class.getName(),
                    context.getAxisConfiguration(), null, "http://cesnet.cz/shongo/rpc-test", "http://cesnet.cz/shongo/rpc-test",
                    ClassLoader.getSystemClassLoader());
            context.getAxisConfiguration().addService(service);

            SimpleHTTPServer server = new SimpleHTTPServer(context, port);
            server.start();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
