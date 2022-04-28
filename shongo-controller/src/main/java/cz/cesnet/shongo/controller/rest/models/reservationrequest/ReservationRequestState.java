package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ExecutableState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * Represents a reservation request state.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestState
{
    /**
     * Reservation request has not been confirmed yet, nor allocated by the scheduler yet.
     */
    CONFIRM_AWAITING(false),

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
     * Reservation request is allocated by the scheduler and the allocated executable has been started and stopped.
     */
    ALLOCATED_FINISHED(true),

    /**
     * Reservation request cannot be allocated by the scheduler or the starting of executable failed.
     */
    FAILED(false),

    /**
     * The reservation request has been denied. It won't be allocated.
     */
    DENIED(false),

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

    public String getMessage(MessageSource messageSource, Locale locale, SpecificationType specificationType)
    {
        return messageSource.getMessage(
                "views.reservationRequest.state." + specificationType + "." + this, null, locale);
    }

    public String getHelp(MessageSource messageSource, Locale locale, SpecificationType specificationType,
            String reservationId)
    {
        String helpMessage = "views.reservationRequest.stateHelp." + specificationType + "." +  this;
        if (this.equals(FAILED) && reservationId != null) {
            return messageSource.getMessage(helpMessage + ".hasReservation", null, locale);
        }
        return messageSource.getMessage(helpMessage, null, locale);
    }

    public String getHelp(MessageSource messageSource, Locale locale, SpecificationType specificationType)
    {
        String helpMessageCode = "views.reservationRequest.stateHelp." + specificationType + "." +  this;
        return messageSource.getMessage(helpMessageCode, null, locale);
    }

    public static ReservationRequestState fromApi(ReservationRequestSummary reservationRequest)
    {
        return fromApi(reservationRequest.getAllocationState(), reservationRequest.getExecutableState(),
                reservationRequest.getUsageExecutableState(), reservationRequest.getType(),
                SpecificationType.fromReservationRequestSummary(reservationRequest),
                reservationRequest.getAllocatedReservationId());
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
            return null;
        }
        switch (allocationState) {
            case ALLOCATED:
                if (executableState != null) {
                    switch (specificationType) {
                        case VIRTUAL_ROOM:
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
                return ALLOCATED;
            case ALLOCATION_FAILED:
                if (reservationRequestType.equals(ReservationRequestType.MODIFIED) && lastReservationId != null) {
                    return MODIFICATION_FAILED;
                }
                else {
                    return FAILED;
                }
            case CONFIRM_AWAITING:
                return CONFIRM_AWAITING;
            case DENIED:
                return DENIED;
            default:
                return NOT_ALLOCATED;
        }
    }
}
