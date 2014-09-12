package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.EndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.specification.StatefulSpecification;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a group of specifications. Every endpoint which will be allocated from the specifications should be
 * interconnected to each other.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentSpecification extends Specification
        implements StatefulSpecification, ReservationTaskProvider
{
    /**
     * List of specifications for targets which are requested to participate in compartment.
     */
    private List<AbstractParticipant> participants = new ArrayList<AbstractParticipant>();

    /**
     * Specifies the default option who should initiate the call ({@code null} means
     * that {@link Scheduler} can decide it).
     */
    private CallInitiation callInitiation;

    /**
     * Constructor.
     */
    public CompartmentSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param callInitiation sets the {@link #callInitiation}
     */
    public CompartmentSpecification(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    /**
     * @return {@link #participants}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AbstractParticipant> getParticipants()
    {
        return Collections.unmodifiableList(participants);
    }

    /**
     * @return all {@link #participants} which aren't instance of {@link StatefulSpecification} or which are instance
     *         of {@link StatefulSpecification} and theirs current state is {@link StatefulSpecification.State#READY}.
     * @throws IllegalStateException when specification is instance of {@link StatefulSpecification} and when then it's
     *                               state is {@link StatefulSpecification.State#NOT_READY}
     */
    @Transient
    public List<AbstractParticipant> getReadyParticipants()
    {
        List<AbstractParticipant> specifications = new ArrayList<AbstractParticipant>();
        for (AbstractParticipant participant : this.participants) {
            if (participant instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) participant;
                switch (statefulSpecification.getCurrentState()) {
                    case SKIP:
                        continue;
                    case READY:
                        break;
                    default:
                        throw new IllegalStateException(String.format("%s should not be in %s state.",
                                participant.getClass().getSimpleName(),
                                statefulSpecification.getCurrentState().toString()));
                }
            }
            specifications.add(participant);
        }
        return specifications;
    }

    /**
     * @param participant to be searched in the {@link CompartmentSpecification}
     * @return true if the {@link CompartmentSpecification} contains given {@code participant},
     *         false otherwise
     */
    public boolean containsParticipant(AbstractParticipant participant)
    {
        Long participantId = participant.getId();
        for (AbstractParticipant possibleParticipant : participants) {
            if (possibleParticipant.getId().equals(participantId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public void removeSpecification(AbstractParticipant participant)
    {
        participants.remove(participant);
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    @Override
    public void updateTechnologies(EntityManager entityManager)
    {
        clearTechnologies();
        for (AbstractParticipant participant : participants) {
            if (participant instanceof EndpointParticipant) {
                EndpointParticipant endpointParticipant = (EndpointParticipant) participant;
                addTechnologies(endpointParticipant.getTechnologies());
            }
            else if (participant instanceof ExternalEndpointSetParticipant) {
                ExternalEndpointSetParticipant endpointParticipant = (ExternalEndpointSetParticipant) participant;
                addTechnologies(endpointParticipant.getTechnologies());
            }
        }
    }

    @Override
    @Transient
    public State getCurrentState()
    {
        State state = State.READY;
        for (AbstractParticipant participant : participants) {
            if (participant instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) participant;
                if (statefulSpecification.getCurrentState().equals(State.NOT_READY)) {
                    state = State.NOT_READY;
                    break;
                }
            }
        }
        return state;
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSame(getCallInitiation(), compartmentSpecification.getCallInitiation());

        setCallInitiation(compartmentSpecification.getCallInitiation());

        // Delete all participants
        for (AbstractParticipant participant : new LinkedList<AbstractParticipant>(participants)) {
            removeSpecification(participant);
        }
        // Add new participants
        for (AbstractParticipant participant : compartmentSpecification.getParticipants()) {
            try {
                addParticipant(participant.clone());
            }
            catch (CloneNotSupportedException exception) {
                throw new RuntimeException(exception);
            }
            modified = true;
        }

        return modified;
    }

    @Override
    public CompartmentReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
    {
        return new CompartmentReservationTask(this, schedulerContext, slot);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.CompartmentSpecification();
    }

    @Override
    public cz.cesnet.shongo.controller.api.CompartmentSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.CompartmentSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.CompartmentSpecification) specificationApi;
        for (AbstractParticipant participant : getParticipants()) {
            compartmentSpecificationApi.addParticipant(participant.toApi());
        }
        compartmentSpecificationApi.setCallInitiation(getCallInitiation());
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.CompartmentSpecification) specificationApi;

        setCallInitiation(compartmentSpecificationApi.getCallInitiation());

        Synchronization.synchronizeCollection(
                participants, compartmentSpecificationApi.getParticipants(),
                new Synchronization.Handler<AbstractParticipant,
                        cz.cesnet.shongo.controller.api.AbstractParticipant>(AbstractParticipant.class)
                {
                    @Override
                    public AbstractParticipant createFromApi(
                            cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                    {
                        return AbstractParticipant.createFromApi(objectApi, entityManager);
                    }

                    @Override
                    public void updateFromApi(AbstractParticipant object,
                            cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                    {
                        object.fromApi(objectApi, entityManager);
                    }
                });

        super.fromApi(specificationApi, entityManager);
    }
}
