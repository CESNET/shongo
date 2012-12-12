package cz.cesnet.shongo.controller.api;

/**
 * Status of a domain.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum Status
{
    /**
     * Means that domain is currently available to the controller.
     */
    AVAILABLE,

    /**
     * Means that domain is currently not available to the controller.
     */
    NOT_AVAILABLE
}
