package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.xmlrpc.Service;

/**
 * Interface to the service handling common operations.
 *
 * @author Martin Srom
 */
public interface CommonService extends Service
{
    /**
     * Get information about controller platform.
     *
     * @return controller information
     */
    public ControllerInfo getControllerInfo();
}
