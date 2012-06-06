package cz.cesnet.shongo.controller.reservation;


import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;

/**
 * Represents a person that is requested to participate in a compartment/compartment request.
 * A requested person will be contacted and the person must express
 * if he/she will participate in the compartment.
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
    private Compartment compartment;

    /**
     * Requested person.
     */
    private Person person;

    /**
     * Current state of contacting the requested person.
     */
    private State state;

    /**
     * @return {@link #compartment}
     */
    @ManyToOne
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
