package cz.cesnet.shongo.rpctest.soapws;

import cz.cesnet.shongo.rpctest.soapws.service.*;
import cz.cesnet.shongo.rpctest.soapws.service.Api;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;
import java.net.MalformedURLException;
import java.net.URL;

public class Client
{
    public static void main(String[] args)
    {
        Api api = null;
        try{
            WebServiceClient annotation = ApiImplService.class.getAnnotation(WebServiceClient.class);
            QName name = new QName(annotation.targetNamespace(), annotation.name());
            ApiImplService service = new ApiImplService(new URL(Server.address), name);
            api = service.getApiImplPort();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Resource resource = api.getResource();
        System.out.println(resource.getName());
        System.out.println(resource.getDescription());

        try {
            System.out.println(api.getMessage());
        } catch (ApiException_Exception e) {
            e.printStackTrace();
        }
        System.out.println(api.div(2324, 34));
    }
}
