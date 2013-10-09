package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Specification} for multiple {@link AliasSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSetSpecification extends Specification
        implements ReservationTaskProvider
{
    /**
     * List of {@link AliasSpecification} for {@link cz.cesnet.shongo.controller.resource.Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

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
        AliasSetSpecification aliasSetSpecification = (AliasSetSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(isSharedExecutable(), aliasSetSpecification.isSharedExecutable());

        setSharedExecutable(aliasSetSpecification.isSharedExecutable());

        if (!aliasSpecifications.equals(aliasSetSpecification.getAliasSpecifications())) {
            setAliasSpecifications(aliasSetSpecification.getAliasSpecifications());
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        AliasSetReservationTask aliasSetReservationTask = new AliasSetReservationTask(schedulerContext);
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            aliasSetReservationTask.addAliasSpecification(aliasSpecification);
        }
        aliasSetReservationTask.setSharedExecutable(isSharedExecutable());
        return aliasSetReservationTask;
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
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.AliasSetSpecification aliasSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSetSpecification) specificationApi;

        setSharedExecutable(aliasSetSpecificationApi.getSharedExecutable());

        Synchronization.synchronizeCollection(aliasSpecifications, aliasSetSpecificationApi.getAliases(),
                new Synchronization.Handler<AliasSpecification, cz.cesnet.shongo.controller.api.AliasSpecification>(
                        AliasSpecification.class)
                {
                    @Override
                    public AliasSpecification createFromApi(
                            cz.cesnet.shongo.controller.api.AliasSpecification objectApi)
                    {
                        AliasSpecification aliasSpecification = new AliasSpecification();
                        aliasSpecification.fromApi(objectApi, entityManager);
                        return aliasSpecification;
                    }

                    @Override
                    public void updateFromApi(AliasSpecification object,
                            cz.cesnet.shongo.controller.api.AliasSpecification objectApi)
                    {
                        object.fromApi(objectApi, entityManager);
                    }
                });

        super.fromApi(specificationApi, entityManager);
    }
}
