package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.controller.common.Person;

import javax.persistence.*;
import java.util.Map;

/**
 * Represents a {@link Specification} for a person.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonSpecification extends Specification
{
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
     * @return {@link #endpointSpecification}
     */
    @OneToOne
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
    public void setState(InvitationState state)
    {
        this.invitationState = state;
    }

    @Override
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
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("person", person.toString());
        map.put("endpoint", (endpointSpecification != null ? endpointSpecification.getId().toString() : null));
        map.put("invitationState", invitationState.toString());
    }
}
