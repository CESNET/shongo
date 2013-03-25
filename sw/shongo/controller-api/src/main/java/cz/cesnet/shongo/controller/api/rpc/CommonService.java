package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.Connector;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.FaultException;

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
    public Collection<Domain> listDomains(SecurityToken token)
            throws FaultException;

    /**
     * @return list of connector
     */
    @API
    public Collection<Connector> listConnectors(SecurityToken token)
            throws FaultException;
}
