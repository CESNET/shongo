package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;

import javax.persistence.*;

/**
 * Represents a {@link Compartment} that is requested for a specific date/time slot.
 * The compartment should be copied to compartment request(s), because each
 * request can be filled by different additional information.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentRequest extends Compartment
{
    /**
     * Enumeration of compartment request state.
     */
    public static enum State
    {
        /**
         * Some of requested persons has {@link PersonRequest.State#NOT_ASKED}
         * or {@link PersonRequest.State#ASKED} state.
          */
        NOT_COMPLETE,

        /**
         * All of the requested persons have {@link PersonRequest.State#ACCEPTED}
         * or {@link PersonRequest.State#REJECTED} state.
         */
        COMPLETE
    }

    /**
     * Date/time slot for which the compartment is requested.
     */
    private AbsoluteDateTimeSlot requestedSlot;

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * @return {@link #requestedSlot}
     */
    @OneToOne
    public AbsoluteDateTimeSlot getRequestedSlot()
    {
        return requestedSlot;
    }

    /**
     * @param requestedSlot sets the {@link #requestedSlot}
     */
    public void setRequestedSlot(AbsoluteDateTimeSlot requestedSlot)
    {
        this.requestedSlot = requestedSlot;
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }
}
