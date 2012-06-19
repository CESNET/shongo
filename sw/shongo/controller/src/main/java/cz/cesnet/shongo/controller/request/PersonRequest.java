package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;
import java.util.Map;

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
    @Access(AccessType.FIELD)
    public CompartmentRequest getCompartmentRequest()
    {
        return compartmentRequest;
    }

    /**
     * @param compartmentRequest sets the {@link #compartmentRequest}
     */
    public void setCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        // Manage bidirectional association
        if (compartmentRequest != this.compartmentRequest) {
            if (this.compartmentRequest != null) {
                CompartmentRequest oldCompartmentRequest = this.compartmentRequest;
                this.compartmentRequest = null;
                oldCompartmentRequest.removeRequestedPerson(this);
            }
            if (compartmentRequest != null) {
                this.compartmentRequest = compartmentRequest;
                this.compartmentRequest.addRequestedPerson(this);
            }
        }
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

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("person", person.toString());
        map.put("resource", resourceSpecification.getId().toString());
        map.put("state", state.toString());
    }
}
