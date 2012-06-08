package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

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
     * @return {@link #requestedSlot}
     */
    @OneToOne(cascade = CascadeType.ALL)
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
    public List<PersonRequest> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPersons sets the {@link #requestedPersons}
     */
    private void setRequestedPersons(List<PersonRequest> requestedPersons)
    {
        this.requestedPersons = requestedPersons;
    }

    /**
     * @param requestedPerson person to be added to the list of requested persons
     */
    public void addRequestedPerson(PersonRequest requestedPerson)
    {
        this.requestedPersons.add(requestedPerson);
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
        //throw new RuntimeException("TODO: Implement CompartmentRequest.updateState");

        setState(State.NOT_COMPLETE);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        //addCollectionToMap(map, "persons", requestedPersons);
        //addCollectionToMap(map, "resources", requestedResources);
    }
}
