package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.executor.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Specification} for multiple {@link AliasSpecification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasGroupSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * List of {@link AliasSpecification} for {@link cz.cesnet.shongo.controller.resource.Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

    /**
     * Constructor.
     */
    public AliasGroupSpecification()
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
     * @throws cz.cesnet.shongo.fault.EntityNotFoundException when the {@link AliasSpecification} doesn't exist
     */
    @Transient
    private AliasSpecification getAliasSpecificationById(Long id) throws EntityNotFoundException
    {
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            if (aliasSpecification.getId().equals(id)) {
                return aliasSpecification;
            }
        }
        throw new EntityNotFoundException(AliasSpecification.class, id);
    }

    /**
     * @param aliasSpecifications sets the {@link #aliasSpecifications}
     */
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications.clear();
        for ( AliasSpecification aliasSpecification : aliasSpecifications) {
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasGroupSpecification roomSpecification = (AliasGroupSpecification) specification;

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
        AliasGroupReservationTask aliasGroupReservationTask = new AliasGroupReservationTask(context);
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            aliasGroupReservationTask.addAliasSpecification(aliasSpecification);
        }
        return aliasGroupReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        for (AliasSpecification aliasSpecification : getAliasSpecifications()) {
            roomSpecificationApi.addAliasSpecification(aliasSpecification.toApi());
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasGroupSpecification aliasGroupSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasGroupSpecification) specificationApi;

        // Create/update alias specifications
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasApi :
                aliasGroupSpecificationApi.getAliasSpecifications()) {
            if (specificationApi.isPropertyItemMarkedAsNew(aliasGroupSpecificationApi.ALIAS_SPECIFICATIONS, aliasApi)) {
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
                specificationApi.getPropertyItemsMarkedAsDeleted(aliasGroupSpecificationApi.ALIAS_SPECIFICATIONS);
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi : aliasSpecificationsToDelete) {
            removeAliasSpecification(getAliasSpecificationById(aliasSpecificationApi.notNullIdAsLong()));
        }

        super.fromApi(specificationApi, entityManager);
    }
}
