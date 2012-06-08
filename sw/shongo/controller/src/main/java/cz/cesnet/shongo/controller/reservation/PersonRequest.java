package cz.cesnet.shongo.controller.reservation;


import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;

/**
 * Represents a person that is requested to participate in a compartment request.
 * A requested person will be contacted and the person must express
 * if he/she will participate in the compartment request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonRequest extends PersistentObject
{
    /**
     * State of contacting the person.
     */
    public static enum State
    {
        /**
         * Person hasn't been contacted yet.
         */
        NOT_ASKED,

        /**
         * Person have been contacted but the person hasn't replied yet.
         */
        ASKED,

        /**
         * Person has accepted the invitation.
         */
        ACCEPTED,

        /**
         * Person has rejected the invitation.
         */
        REJECTED
    }

    /**
     * Compartment in which the person request is located.
     */
    private CompartmentRequest compartmentRequest;

    /**
     * Requested person.
     */
    private Person person;

    /**
     * Specified resource by which the person connects to compartment.
     */
    private ResourceSpecification resourceSpecification;

    /**
     * Current state of contacting the requested person.
     */
    private State state;

    /**
     * @return {@link #compartmentRequest}
     */
    @ManyToOne
    public CompartmentRequest getCompartmentRequest()
    {
        return compartmentRequest;
    }

    /**
     * @param compartmentRequest sets the {@link #compartmentRequest}
     */
    public void setCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        this.compartmentRequest = compartmentRequest;
    }

    /**
     * @return {@link #person}
     */
    @OneToOne
    public Person getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #person}
     */
    public void setPerson(Person person)
    {
        this.person = person;
    }

    /**
     * @return {@link #resourceSpecification}
     */
    @OneToOne
    public ResourceSpecification getResourceSpecification()
    {
        return resourceSpecification;
    }

    /**
     * @param resourceSpecification sets the {@link #resourceSpecification}
     */
    public void setResourceSpecification(ResourceSpecification resourceSpecification)
    {
        this.resourceSpecification = resourceSpecification;
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
