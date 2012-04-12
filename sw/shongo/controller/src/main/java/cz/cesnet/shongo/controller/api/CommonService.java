package cz.cesnet.shongo.controller.api;

/**
 * Interface to the service handling common operations.
 *
 * @author Martin Srom
 */
public interface CommonService
{
    /**
     * Get information about controller platform.
     *
     * @return controller information
     */
    public ControllerInfo getControllerInfo();
}
