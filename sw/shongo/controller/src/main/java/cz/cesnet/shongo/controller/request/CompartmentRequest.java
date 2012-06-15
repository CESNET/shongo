package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link Compartment} that is requested for a specific date/time slot.
 * The compartment should be copied to compartment request(s), because each
 * request can be filled by different additional information.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentRequest extends PersistentObject
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
     * Reservation request for which the request is made.
     */
    private ReservationRequest reservationRequest;

    /**
     * Compartment for which the request is made.
     */
    private Compartment compartment;

    /**
     * Date/time slot for which the compartment is requested.
     */
    private AbsoluteDateTimeSlot requestedSlot;

    /**
     * List of persons which are requested to participate in compartment.
     */
    private List<PersonRequest> requestedPersons = new ArrayList<PersonRequest>();

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * @return {@link #compartment}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        this.compartment = compartment;
    }

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public ReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(ReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return {@link #requestedSlot}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
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
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartmentRequest")
    @Access(AccessType.FIELD)
    public List<PersonRequest> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPerson person to be added to the {@link #requestedPersons}
     */
    public void addRequestedPerson(PersonRequest requestedPerson)
    {
        // Manage bidirectional association
        if (requestedPersons.contains(requestedPerson) == false) {
            requestedPersons.add(requestedPerson);
            requestedPerson.setCompartmentRequest(this);
        }
    }

    /**
     * @param requestedPerson person to be removed from the {@link #requestedPersons}
     */
    public void removeRequestedPerson(PersonRequest requestedPerson)
    {
        // Manage bidirectional association
        if (requestedPersons.contains(requestedPerson)) {
            requestedPersons.remove(requestedPerson);
            requestedPerson.setCompartmentRequest(null);
        }
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

    /**
     * Update state of the compartment request based on requested persons.
     * @see State
     */
    public void updateState()
    {
        State state = State.COMPLETE;
        for ( PersonRequest personRequest : requestedPersons ) {
            PersonRequest.State personRequestState = personRequest.getState();
            if ( personRequestState == PersonRequest.State.NOT_ASKED
                    || personRequestState == PersonRequest.State.ASKED ) {
                state = State.NOT_COMPLETE;
            }
        }
        setState(state);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("compartment", compartment.toString());
        map.put("slot", requestedSlot.toString());
        map.put("state", state.toString());
        addCollectionToMap(map, "persons", requestedPersons);
    }
}
