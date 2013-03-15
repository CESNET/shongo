package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.ControllerImplFaultSet;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.scheduler.AliasSetReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SpecificationCheckAvailability;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link Specification} for multiple {@link AliasSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSetSpecification extends Specification
        implements ReservationTaskProvider, SpecificationCheckAvailability
{
    /**
     * List of {@link AliasSpecification} for {@link cz.cesnet.shongo.controller.resource.Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

    /**
     * Share created executable.
     */
    private boolean sharedExecutable;

    /**
     * Constructor.
     */
    public AliasSetSpecification()
    {
    }

    /**
     * @return {@link #aliasSpecifications}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AliasSpecification> getAliasSpecifications()
    {
        return Collections.unmodifiableList(aliasSpecifications);
    }

    /**
     * @param id of the requested {@link AliasSpecification}
     * @return {@link AliasSpecification} with given {@code id}
     * @throws FaultException when the {@link AliasSpecification} doesn't exist
     */
    @Transient
    private AliasSpecification getAliasSpecificationById(Long id) throws FaultException
    {
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            if (aliasSpecification.getId().equals(id)) {
                return aliasSpecification;
            }
        }
        return ControllerImplFaultSet.throwEntityNotFoundFault(AliasSpecification.class, id);
    }

    /**
     * @param aliasSpecifications sets the {@link #aliasSpecifications}
     */
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications.clear();
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            this.aliasSpecifications.add(aliasSpecification.clone());
        }
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #aliasSpecifications}
     */
    public void removeAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.remove(aliasSpecification);
    }

    /**
     * @return {@link #sharedExecutable}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isSharedExecutable()
    {
        return sharedExecutable;
    }

    /**
     * @param sharedExecutable sets the {@link #sharedExecutable}
     */
    public void setSharedExecutable(boolean sharedExecutable)
    {
        this.sharedExecutable = sharedExecutable;
    }

    @Override
    public void updateTechnologies()
    {
        clearTechnologies();
        for (AliasSpecification specification : aliasSpecifications) {
            addTechnologies(specification.getTechnologies());
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasSetSpecification roomSpecification = (AliasSetSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);

        if (!aliasSpecifications.equals(roomSpecification.getAliasSpecifications())) {
            setAliasSpecifications(roomSpecification.getAliasSpecifications());
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        AliasSetReservationTask aliasSetReservationTask = new AliasSetReservationTask(context);
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            aliasSetReservationTask.addAliasSpecification(aliasSpecification);
        }
        aliasSetReservationTask.setSharedExecutable(isSharedExecutable());
        return aliasSetReservationTask;
    }

    @Override
    public void checkAvailability(Interval slot, EntityManager entityManager) throws ReportException
    {
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            aliasSpecification.checkAvailability(slot, entityManager);
        }
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasSetSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.AliasSetSpecification aliasSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSetSpecification) specificationApi;
        for (AliasSpecification aliasSpecification : getAliasSpecifications()) {
            aliasSetSpecificationApi.addAlias(aliasSpecification.toApi());
        }
        aliasSetSpecificationApi.setSharedExecutable(isSharedExecutable());
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasSetSpecification aliasSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSetSpecification) specificationApi;

        // Create/update alias specifications
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasApi :
                aliasSetSpecificationApi.getAliases()) {
            if (specificationApi.isPropertyItemMarkedAsNew(aliasSetSpecificationApi.ALIASES, aliasApi)) {
                AliasSpecification aliasSpecification = new AliasSpecification();
                aliasSpecification.fromApi(aliasApi, entityManager);
                addAliasSpecification(aliasSpecification);
            }
            else {
                AliasSpecification aliasSpecification = getAliasSpecificationById(aliasApi.notNullIdAsLong());
                aliasSpecification.fromApi(aliasApi, entityManager);
            }
        }
        // Delete room settings
        Set<cz.cesnet.shongo.controller.api.AliasSpecification> aliasSpecificationsToDelete =
                specificationApi.getPropertyItemsMarkedAsDeleted(aliasSetSpecificationApi.ALIASES);
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi : aliasSpecificationsToDelete) {
            removeAliasSpecification(getAliasSpecificationById(aliasSpecificationApi.notNullIdAsLong()));
        }

        if (aliasSetSpecificationApi.isPropertyFilled(aliasSetSpecificationApi.SHARED_EXECUTABLE)) {
            setSharedExecutable(aliasSetSpecificationApi.getSharedExecutable());
        }

        super.fromApi(specificationApi, entityManager);
    }
}
