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
     * None reservation has been allocated for the earliest requested slot.
     * Allocation is awaiting for owner confirmation first.
     */
    CONFIRM_AWAITING,

    /**
     * The reservation has been allocated for the earliest requested slot.
     */
    ALLOCATED,

    /**
     * The reservation has failed to be allocated for the earliest requested slot.
     */
    ALLOCATION_FAILED,

    /**
     * The reservation request has been denied. It won't be allocated.
     */
    DENIED;
}
