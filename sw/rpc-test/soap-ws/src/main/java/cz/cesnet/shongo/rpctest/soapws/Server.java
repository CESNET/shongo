package cz.cesnet.shongo.rpctest.soapws;

import javax.xml.ws.Endpoint;

public class Server
{
    public static final String address = "http://localhost:8008/api";

    public static void main(String[] args)
    {
        Endpoint.publish(address, new ApiImpl());
    }
}


