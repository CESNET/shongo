package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.scheduler.CompartmentReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

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
    public Collection<Specification> getChildSpecifications()
    {
        Collection<Specification> specifications = new ArrayList<Specification>();
        for (ParticipantSpecification participantSpecification : this.specifications) {
            specifications.add(participantSpecification);
        }
        return specifications;
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
     * @throws EntityNotFoundException when the {@link Specification} doesn't exist
     */
    private ParticipantSpecification getSpecificationById(Long id) throws EntityNotFoundException
    {
        for (ParticipantSpecification specification : specifications) {
            if (specification.getId().equals(id)) {
                return specification;
            }
        }
        throw new EntityNotFoundException(ParticipantSpecification.class, id);
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

        boolean modified = false;
        modified |= !ObjectUtils.equals(getCallInitiation(), compartmentSpecification.getCallInitiation());

        setCallInitiation(compartmentSpecification.getCallInitiation());

        return modified;
    }

    @Override
    public CompartmentReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new CompartmentReservationTask(this, context);
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.CompartmentSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.CompartmentSpecification) specificationApi;
        for (ParticipantSpecification specification : getSpecifications()) {
            compartmentSpecificationApi.addSpecification(specification.toApi(domain));
        }
        compartmentSpecificationApi.setCallInitiation(getCallInitiation());
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecificationApi =
                (cz.cesnet.shongo.controller.api.CompartmentSpecification) specificationApi;
        if (compartmentSpecificationApi.isPropertyFilled(compartmentSpecificationApi.CALL_INITIATION)) {
            setCallInitiation(compartmentSpecificationApi.getCallInitiation());
        }

        // Create/modify specifications
        for (cz.cesnet.shongo.controller.api.Specification specApi : compartmentSpecificationApi.getSpecifications()) {
            if (compartmentSpecificationApi.isCollectionItemMarkedAsNew(
                    compartmentSpecificationApi.SPECIFICATIONS, specApi)) {
                addChildSpecification(Specification.createFromApi(specApi, entityManager, domain));
            }
            else {
                Specification specification = getSpecificationById(specApi.notNullIdAsLong());
                specification.fromApi(specApi, entityManager, domain);
            }
        }
        // Delete specifications
        Set<cz.cesnet.shongo.controller.api.Specification> apiDeletedSpecifications =
                compartmentSpecificationApi.getCollectionItemsMarkedAsDeleted(
                        compartmentSpecificationApi.SPECIFICATIONS);
        for (cz.cesnet.shongo.controller.api.Specification specApi : apiDeletedSpecifications) {
            removeSpecification(getSpecificationById(specApi.notNullIdAsLong()));
        }

        super.fromApi(specificationApi, entityManager, domain);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("callInitiation", callInitiation);
        map.put("specifications", specifications);
    }
}
