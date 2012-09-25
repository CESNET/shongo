package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Interface to the service handling control operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceControlService extends Service
{
    @API
    public String dial(SecurityToken token, String deviceResourceIdentifier, String target) throws FaultException;

    @API
    public String standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException;
}
