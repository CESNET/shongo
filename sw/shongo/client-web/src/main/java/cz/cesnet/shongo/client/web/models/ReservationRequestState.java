package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ReservationRequestType;

/**
 * Represents a reservation request state.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestState
{
    /**
     * Reservation request has not been allocated by the scheduler yet.
     */
    NOT_ALLOCATED(false),

    /**
     * Reservation request is allocated by the scheduler but the allocated executable has not been started yet.
     */
    ALLOCATED(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated executable is started.
     */
    ALLOCATED_STARTED(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated room is started but not available for participants to join.
     */
    ALLOCATED_STARTED_NOT_AVAILABLE(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated room is started and available for participants to join.
     */
    ALLOCATED_STARTED_AVAILABLE(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated capacity is available for participants to join.
     */
    ALLOCATED_AVAILABLE(true),

    /**
     * Reservation request is allocated by the scheduler and the allocated executable has been started and stopped.
     */
    ALLOCATED_FINISHED(true),

    /**
     * Reservation request cannot be allocated by the scheduler or the starting of executable failed.
     */
    FAILED(false),

    /**
     * Modification of reservation request cannot be allocated by the scheduler
     * but some previous version of reservation request has been allocated and started.
     */
    MODIFICATION_FAILED(false);

    /**
     * Specifies whether reservation request is allocated.
     */
    private final boolean allocated;

    /**
     * Constructor.
     *
     * @param allocated sets the {@link #allocated}
     */
    private ReservationRequestState(boolean allocated)
    {
        this.allocated = allocated;
    }

    /**
     * @return {@link #allocated}
     */
    public boolean isAllocated()
    {
        return allocated;
    }

    public static ReservationRequestState fromApi(ReservationRequestSummary reservationRequest)
    {
        return fromApi(reservationRequest.getAllocationState(), reservationRequest.getExecutableState(),
                reservationRequest.getUsageExecutableState(), reservationRequest.getType(),
                SpecificationType.fromReservationRequestSummary(reservationRequest),
                reservationRequest.getLastReservationId());
    }

    /**
     * @param allocationState
     * @param executableState
     * @param usageExecutableState
     * @param reservationRequestType
     * @param specificationType
     * @param lastReservationId
     * @return {@link ReservationRequestState}
     */
    public static ReservationRequestState fromApi(AllocationState allocationState, ExecutableState executableState,
            ExecutableState usageExecutableState, ReservationRequestType reservationRequestType,
            SpecificationType specificationType, String lastReservationId)
    {
        if (allocationState == null) {
            return NOT_ALLOCATED;
        }
        switch (allocationState) {
            case ALLOCATED:
                if (executableState == null) {
                    return ALLOCATED;
                }
                else {
                    switch (specificationType) {
                        case PERMANENT_ROOM:
                            switch (executableState) {
                                case STARTED:
                                    if (usageExecutableState != null && usageExecutableState.isAvailable()) {
                                        return ALLOCATED_STARTED_AVAILABLE;
                                    }
                                    else {
                                        return ALLOCATED_STARTED_NOT_AVAILABLE;
                                    }
                                case STOPPED:
                                case STOPPING_FAILED:
                                    return ALLOCATED_FINISHED;
                                case STARTING_FAILED:
                                    return FAILED;
                                default:
                                    return ALLOCATED;
                            }
                        case PERMANENT_ROOM_CAPACITY:
                            switch (executableState) {
                                case STARTED:
                                    return ALLOCATED_AVAILABLE;
                                case STOPPED:
                                case STOPPING_FAILED:
                                    return ALLOCATED_FINISHED;
                                case STARTING_FAILED:
                                    return FAILED;
                                default:
                                    return ALLOCATED;
                            }
                        case ADHOC_ROOM:
                            switch (executableState) {
                                case STARTED:
                                    return ALLOCATED_STARTED;
                                case STOPPED:
                                case STOPPING_FAILED:
                                    return ALLOCATED_FINISHED;
                                case STARTING_FAILED:
                                    return FAILED;
                                default:
                                    return ALLOCATED;
                            }
                    }
                }
            case ALLOCATION_FAILED:
                if (reservationRequestType.equals(ReservationRequestType.MODIFIED) && lastReservationId != null) {
                    return MODIFICATION_FAILED;
                }
                else {
                    return FAILED;
                }
            default:
                return NOT_ALLOCATED;
        }
    }
}
