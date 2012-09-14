package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.scheduler.PersonReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.Map;

/**
 * Represents a {@link Specification} for a person.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonSpecification extends Specification implements StatefulSpecification
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
            case INVITATION_ACCEPTED:
                return State.READY;
            case INVITATION_REJECTED:
                return State.SKIP;
            default:
                return State.NOT_READY;
        }
    }

    @Override
    public PersonSpecification clone(Map<Specification, Specification> originalSpecifications)
    {
        PersonSpecification personSpecification = new PersonSpecification();
        personSpecification.setPerson(getPerson());
        personSpecification.setEndpointSpecification(getEndpointSpecification());
        personSpecification.setInvitationState(getInvitationState());

        originalSpecifications.put(personSpecification, this);

        return personSpecification;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        PersonSpecification personSpecification = (PersonSpecification) specification;

        boolean modified = false;
        modified |= !ObjectUtils.equals(getPerson(), personSpecification.getPerson())
                || !ObjectUtils.equals(getEndpointSpecification(), personSpecification.getEndpointSpecification())
                || !ObjectUtils.equals(getInvitationState(), personSpecification.getInvitationState());

        setPerson(personSpecification.getPerson());
        setEndpointSpecification(personSpecification.getEndpointSpecification());
        setInvitationState(personSpecification.getInvitationState());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new PersonReservationTask(this, context);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("person", person);
        map.put("endpoint", endpointSpecification);
        map.put("invitationState", invitationState);
    }

    @PrePersist
    protected void onCreate()
    {
        if (invitationState == null) {
            invitationState = InvitationState.INVITATION_NOT_SENT;
        }
    }

    /**
     * State of contacting the person.
     */
    public static enum InvitationState
    {
        /**
         * Person hasn't been invited yet.
         */
        INVITATION_NOT_SENT,

        /**
         * Person has been invited but the person hasn't replied yet.
         */
        INVITATION_SENT,

        /**
         * Person has accepted the invitation.
         */
        INVITATION_ACCEPTED,

        /**
         * Person has rejected the invitation.
         */
        INVITATION_REJECTED
    }
}
