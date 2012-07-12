package cz.cesnet.shongo.controller.api;

/**
 * Interface to the service handling common operations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface CommonService extends Service
{
    /**
     * Get information about controller platform.
     *
     * @return controller information
     */
    @API
    public ControllerInfo getControllerInfo();
}
