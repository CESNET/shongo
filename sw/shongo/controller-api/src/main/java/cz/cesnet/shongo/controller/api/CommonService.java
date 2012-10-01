package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.Service;

import java.util.Collection;

/**
 * Interface to the service handling common operations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface CommonService extends Service
{
    /**
     * @return information about the domain controller
     */
    @API
    public Controller getController();

    /**
     * @return array of all known domains
     */
    @API
    public Collection<Domain> listDomains(SecurityToken token);

    /**
     * @return
     */
    @API
    public Collection<Connector> listConnectors(SecurityToken token);
}
