package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Represents a {@link Specification} for a person.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonSpecification extends ParticipantSpecification implements StatefulSpecification
{
    /**
     * Requested person.
     */
    private Person person;

    /**
     * {@link EndpointSpecification} which the person use.
     */
    private EndpointSpecification endpointSpecification;

    /**
     * Current state of contacting the requested person.
     */
    private InvitationState invitationState;

    /**
     * Constructor.
     */
    public PersonSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param person sets the {@link #person}
     */
    public PersonSpecification(Person person)
    {
        this.person = person;
    }

    /**
     * Constructor.
     *
     * @param person                sets the {@link #person}
     * @param endpointSpecification sets the {@link #endpointSpecification}
     */
    public PersonSpecification(Person person, EndpointSpecification endpointSpecification)
    {
        this.person = person;
        this.endpointSpecification = endpointSpecification;
    }

    /**
     * @return {@link #person}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
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
     * @return {@link #endpointSpecification}
     */
    @OneToOne(cascade = CascadeType.ALL)
    public EndpointSpecification getEndpointSpecification()
    {
        return endpointSpecification;
    }

    /**
     * @param endpointSpecification sets the {@link #endpointSpecification}
     */
    public void setEndpointSpecification(EndpointSpecification endpointSpecification)
    {
        this.endpointSpecification = endpointSpecification;
    }

    /**
     * @return {@link #invitationState}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public InvitationState getInvitationState()
    {
        return invitationState;
    }

    /**
     * @param state sets the {@link #invitationState}
     */
    public void setInvitationState(InvitationState state)
    {
        this.invitationState = state;
    }

    @Override
    @Transient
    public State getCurrentState()
    {
        switch (invitationState) {
            case ACCEPTED:
                return State.READY;
            case REJECTED:
                return State.SKIP;
            default:
                return State.NOT_READY;
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        PersonSpecification personSpecification = (PersonSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getPerson(), personSpecification.getPerson());

        setPerson(personSpecification.getPerson().clone());

        if (getEndpointSpecification() != personSpecification.getEndpointSpecification()) {
            // We want make change only in the following scenarios
            if (getEndpointSpecification() == null || invitationState != InvitationState.ACCEPTED) {
                setEndpointSpecification(
                        (EndpointSpecification) personSpecification.getEndpointSpecification().clone());
                modified = true;
            }
        }

        if (modified) {
            setInvitationState(InvitationState.NOT_SENT);
        }

        return modified;
    }

    @PrePersist
    protected void onCreate()
    {
        if (invitationState == null) {
            invitationState = InvitationState.NOT_SENT;
        }
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.PersonSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.PersonSpecification personSpecification =
                (cz.cesnet.shongo.controller.api.PersonSpecification) specificationApi;
        personSpecification.setPerson(getPerson().toApi());
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.PersonSpecification personSpecificationApi =
                (cz.cesnet.shongo.controller.api.PersonSpecification) specificationApi;

        cz.cesnet.shongo.controller.api.Person personApi = personSpecificationApi.getPerson();
        if (personApi == null) {
            throw new IllegalArgumentException("Person must not be null.");
        }
        else if (getPerson() != null && getPerson().getId().equals(personApi.notNullIdAsLong())) {
            getPerson().fromApi(personApi);
        }
        else {
            Person person = Person.createFromApi(personApi);
            setPerson(person);
        }

        if (invitationState == null) {
            invitationState = InvitationState.NOT_SENT;
        }

        super.fromApi(specificationApi, entityManager);
    }

    /**
     * State of contacting the person.
     */
    public static enum InvitationState
    {
        /**
         * Person hasn't been invited yet.
         */
        NOT_SENT,

        /**
         * Person has been invited but the person hasn't replied yet.
         */
        SENT,

        /**
         * Person has accepted the invitation.
         */
        ACCEPTED,

        /**
         * Person has rejected the invitation.
         */
        REJECTED
    }
}
