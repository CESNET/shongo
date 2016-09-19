package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.specification.Specification} for multiple {@link AliasSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSetSpecification extends Specification
        implements ReservationTaskProvider
{
    /**
     * List of {@link AliasSpecification} for {@link Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

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
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications, EntityManager entityManager)
    {
        this.aliasSpecifications.clear();
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            this.aliasSpecifications.add(aliasSpecification.clone(entityManager));
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

    @Override
    public void updateTechnologies(EntityManager entityManager)
    {
        clearTechnologies();
        for (AliasSpecification specification : aliasSpecifications) {
            addTechnologies(specification.getTechnologies());
        }
    }

    @Override
    public void updateSpecificationSummary(EntityManager entityManager, boolean deleteOnly, boolean flush)
    {
        super.updateSpecificationSummary(entityManager, deleteOnly, flush);
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.updateSpecificationSummary(aliasSpecification, deleteOnly);
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        AliasSetSpecification aliasSetSpecification = (AliasSetSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);

        if (!ObjectHelper.isSameIgnoreOrder(aliasSpecifications, aliasSetSpecification.getAliasSpecifications())) {
            setAliasSpecifications(aliasSetSpecification.getAliasSpecifications(), entityManager);
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot)
            throws SchedulerException
    {
        AliasSetReservationTask aliasSetReservationTask = new AliasSetReservationTask(schedulerContext, slot);
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            aliasSetReservationTask.addAliasSpecification(aliasSpecification);
        }
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
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.AliasSetSpecification aliasSetSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSetSpecification) specificationApi;

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
