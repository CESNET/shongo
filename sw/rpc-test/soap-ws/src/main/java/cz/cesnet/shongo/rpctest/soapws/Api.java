package cz.cesnet.shongo.rpctest.soapws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.WebFault;

@WebService
public interface Api
{
    @WebFault(name = "ApiException", targetNamespace = "http://soapws.rpctest.shongo.cesnet.cz/")
    public static class ApiException extends Exception
    {
        private String dataLoad;

        public ApiException(String message, String dataLoad)
        {
            super(message);
            this.dataLoad = dataLoad;
        }
        
        public String getDataLoad() {
            return dataLoad;
        }

        public String toString()
        {
            String output = super.toString();
            return output + " [" + dataLoad + "]";
        }
    }

    @WebMethod
    public int add(@WebParam(name="a") int a, @WebParam(name="b") int b);

    @WebMethod
    public int div(@WebParam(name="a") int a, @WebParam(name="b") int b);

    @WebMethod
    public String getMessage() throws ApiException;
}