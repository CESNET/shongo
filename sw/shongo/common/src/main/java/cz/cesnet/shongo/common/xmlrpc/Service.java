package cz.cesnet.shongo.common.xmlrpc;

/**
 * Represents a service that can perform
 * XML-RPC requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Service
{
    /**
     * Get service name
     *
     * @return service name
     */
    public abstract String getServiceName();
}
