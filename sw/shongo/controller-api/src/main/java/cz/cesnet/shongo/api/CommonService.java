package cz.cesnet.shongo.api;

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
    public ControllerInfo getControllerInfo();
}
