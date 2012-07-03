package cz.cesnet.shongo.controller.api;

/**
 * Represents an API service.
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
