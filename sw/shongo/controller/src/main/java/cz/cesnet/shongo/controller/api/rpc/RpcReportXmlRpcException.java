package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.ApiFault;
import org.apache.xmlrpc.XmlRpcException;

/**
 * Method invocation throws exception with {@link AbstractReport} which isn't {@link ApiFault}.
 */
class RpcReportXmlRpcException extends XmlRpcException
{
    private RpcRequestContext requestContext;

    private AbstractReport report;

    public RpcReportXmlRpcException(RpcRequestContext requestContext, AbstractReport report, Throwable throwable)
    {
        super(null, throwable);
        this.requestContext = requestContext;
        this.report = report;
    }

    public RpcRequestContext getRequestContext()
    {
        return requestContext;
    }

    public AbstractReport getReport()
    {
        return report;
    }
}
