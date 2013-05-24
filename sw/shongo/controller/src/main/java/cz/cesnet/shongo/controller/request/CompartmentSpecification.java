package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.scheduler.CompartmentReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.util.ObjectHelper;

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
        implements StatefulSpecification, CompositeSpecification, ReservationTaskProvider
{
    /**
     * List of specifications for targets which are requested to participate in compartment.
     */
    private List<ParticipantSpecification> specifications = new ArrayList<ParticipantSpecification>();

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
     * @return {@link #specifications}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<ParticipantSpecification> getSpecifications()
    {
        return Collections.unmodifiableList(specifications);
    }

    @Override
    @Transient
    public List<? extends Specification> getChildSpecifications()
    {
        return Collections.unmodifiableList(specifications);
    }

    /**
     * @return all {@link #specifications} which aren't instance of {@link StatefulSpecification} or which are instance
     *         of {@link StatefulSpecification} and theirs current state is {@link StatefulSpecification.State#READY}.
     * @throws IllegalStateException when specification is instance of {@link StatefulSpecification} and when then it's
     *                               state is {@link StatefulSpecification.State#NOT_READY}
     */
    @Transient
    public List<ParticipantSpecification> getReadySpecifications()
    {
        List<ParticipantSpecification> specifications = new ArrayList<ParticipantSpecification>();
        for (ParticipantSpecification specification : this.specifications) {
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                switch (statefulSpecification.getCurrentState()) {
                    case SKIP:
                        continue;
                    case READY:
                        break;
                    default:
                        throw new IllegalStateException(String.format("%s should not be in %s state.",
                                specification.getClass().getSimpleName(),
                                statefulSpecification.getCurrentState().toString()));
                }
            }
            specifications.add(specification);
        }
        return specifications;
    }

    /**
     * @param id of the requested {@link Specification}
     * @return {@link Specification} with given {@code id}
     * @throws CommonReportSet.EntityNotFoundException
     *          when the {@link Specification} doesn't exist
     */
    @Transient
    private ParticipantSpecification getSpecificationById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (ParticipantSpecification specification : specifications) {
            if (specification.getId().equals(id)) {
                return specification;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(ParticipantSpecification.class, id);
    }

    /**
     * @param specification to be searched in the {@link CompartmentSpecification}
     * @return true if the {@link CompartmentSpecification} contains given {@code specification},
     *         false otherwise
     */
    public boolean containsSpecification(ParticipantSpecification specification)
    {
        Long specificationId = specification.getId();
        for (Specification possibleSpecification : specifications) {
            if (possibleSpecification.getId().equals(specificationId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param specification to be added to the {@link #specifications}
     */
    public void addSpecification(ParticipantSpecification specification)
    {
        specifications.add(specification);
    }

    /**
     * @param specification to be removed from the {@link #specifications}
     */
    public void removeSpecification(ParticipantSpecification specification)
    {
        specifications.remove(specification);
    }

    @Override
    public void addChildSpecification(Specification specification)
    {
        addSpecification((ParticipantSpecification) specification);
    }

    @Override
    public void removeChildSpecification(Specification specification)
    {
        removeSpecification((ParticipantSpecification) specification);
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
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
    public void updateTechnologies()
    {
        clearTechnologies();
        for (ParticipantSpecification specification : specifications) {
            addTechnologies(specification.getTechnologies());
        }
    }

    @Override
    @Transient
    public State getCurrentState()
    {
        State state = State.READY;
        for (Specification specification : specifications) {
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                if (statefulSpecification.getCurrentState().equals(State.NOT_READY)) {
                    state = State.NOT_READY;
                    break;
                }
            }
        }
        return state;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getCallInitiation(), compartmentSpecification.getCallInitiation());

        setCallInitiation(compartmentSpecification.getCallInitiation());

        return modified;
    }

    @Override
    public CompartmentReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        return new CompartmentReservationTask(this, schedulerContext);
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
        for (ParticipantSpecification specification : getSpecifications()) {
            compartmentSpecificationApi.addSpecification(specification.toApi());
        }
        compartmentSpecificationApi.setCallInitiation(getCallInitiation());
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.CompartmentSpecification) specificationApi;
        if (compartmentSpecificationApi.isPropertyFilled(compartmentSpecificationApi.CALL_INITIATION)) {
            setCallInitiation(compartmentSpecificationApi.getCallInitiation());
        }

        // Create/modify specifications
        for (cz.cesnet.shongo.controller.api.Specification specApi : compartmentSpecificationApi.getSpecifications()) {
            if (compartmentSpecificationApi.isPropertyItemMarkedAsNew(
                    compartmentSpecificationApi.SPECIFICATIONS, specApi)) {
                addChildSpecification(Specification.createFromApi(specApi, entityManager));
            }
            else {
                Specification specification = getSpecificationById(specApi.notNullIdAsLong());
                specification.fromApi(specApi, entityManager);
            }
        }
        // Delete specifications
        Set<cz.cesnet.shongo.controller.api.Specification> apiDeletedSpecifications =
                compartmentSpecificationApi.getPropertyItemsMarkedAsDeleted(
                        compartmentSpecificationApi.SPECIFICATIONS);
        for (cz.cesnet.shongo.controller.api.Specification specApi : apiDeletedSpecifications) {
            removeSpecification(getSpecificationById(specApi.notNullIdAsLong()));
        }

        super.fromApi(specificationApi, entityManager);
    }
}
