package cz.cesnet.shongo.common.xmlrpc;

/**
 * Represents a service that can handle
 * XML-RPC requests.
 *
 * @author Martin Srom
 */
public interface Service
{
    /**
     * Get service name
     *
     * @return service name
     */
    public String getServiceName();
}
