package cz.cesnet.shongo.controller.booking.participant;


import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.specification.StatefulSpecification;

import javax.persistence.*;
import java.util.HashSet;

/**
 * Represents a {@link PersonParticipant} with invitation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class InvitedPersonParticipant extends PersonParticipant implements StatefulSpecification
{
    /**
     * {@link EndpointParticipant} which the person use.
     */
    private EndpointParticipant endpointParticipant;

    /**
     * Current state of contacting the requested person.
     */
    private InvitationState invitationState;

    /**
     * Constructor.
     */
    public InvitedPersonParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param person sets the {@link #person}
     */
    public InvitedPersonParticipant(AbstractPerson person)
    {
        setPerson(person);
    }

    /**
     * Constructor.
     *
     * @param person                sets the {@link #person}
     * @param endpointParticipant sets the {@link #endpointParticipant}
     */
    public InvitedPersonParticipant(AbstractPerson person, EndpointParticipant endpointParticipant)
    {
        setPerson(person);
        this.endpointParticipant = endpointParticipant;
    }

    /**
     * @return {@link #endpointParticipant}
     */
    @OneToOne(cascade = CascadeType.ALL)
    public EndpointParticipant getEndpointParticipant()
    {
        return endpointParticipant;
    }

    /**
     * @param endpointParticipant sets the {@link #endpointParticipant}
     */
    public void setEndpointParticipant(EndpointParticipant endpointParticipant)
    {
        this.endpointParticipant = endpointParticipant;
    }

    /**
     * @return {@link #invitationState}
     */
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
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
    protected void cloneReset()
    {
        super.cloneReset();
        endpointParticipant = null;
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        InvitedPersonParticipant invitedPersonParticipant = (InvitedPersonParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);

        if (getEndpointParticipant() != invitedPersonParticipant.getEndpointParticipant()) {
            // We want make change only in the following scenarios
            if (getEndpointParticipant() == null || invitationState != InvitationState.ACCEPTED) {
                try {
                    setEndpointParticipant(
                            (EndpointParticipant) invitedPersonParticipant.getEndpointParticipant().clone());
                }
                catch (CloneNotSupportedException exception) {
                    throw new RuntimeException(exception);
                }
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
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.InvitedPersonParticipant();
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        if (invitationState == null) {
            invitationState = InvitationState.NOT_SENT;
        }
        super.fromApi(participantApi, entityManager);
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
