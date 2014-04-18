package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.report.ApiFault;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Method invocation throws exception which is {@link cz.cesnet.shongo.report.ApiFault}.
 */
class RpcApiFaultXmlRpcException extends XmlRpcException
{
    private RpcRequestContext requestContext;

    private ApiFault apiFault;

    public RpcApiFaultXmlRpcException(RpcRequestContext requestContext, ApiFault apiFault, Throwable throwable)
    {
        super(null, throwable);
        this.requestContext = requestContext;
        this.apiFault = apiFault;
    }

    public RpcRequestContext getRequestContext()
    {
        return requestContext;
    }

    public ApiFault getApiFault()
    {
        return apiFault;
    }
}
