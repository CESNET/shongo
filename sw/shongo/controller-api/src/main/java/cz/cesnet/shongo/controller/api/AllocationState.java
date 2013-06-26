package cz.cesnet.shongo.controller.api;

/**
 * Allocation state of reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum AllocationState
{
    /**
     * None reservation has been allocated for the earliest requested slot.
     */
    NOT_ALLOCATED,

    /**
     * The reservation has been allocated for the earliest requested slot.
     */
    ALLOCATED,

    /**
     * The reservation has failed to be allocated for the earliest requested slot.
     */
    ALLOCATION_FAILED
}
