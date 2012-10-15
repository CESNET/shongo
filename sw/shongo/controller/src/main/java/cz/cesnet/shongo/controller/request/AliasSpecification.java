package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

/**
 * Represents a {@link cz.cesnet.shongo.controller.request.Specification} for a person.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * {@link Technology} for the {@link Alias}.
     */
    private Technology technology;

    /**
     * {@link AliasType} for the {@link Alias}.
     */
    private AliasType aliasType;

    /**
     * {@link Resource} with {@link AliasProviderCapability}.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #technology}
     * @param aliasType  sets the {@link #aliasType}
     */
    public AliasSpecification(Technology technology, AliasType aliasType)
    {
        this.setTechnology(technology);
        this.setAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #technology}
     * @param resource   sets the {@link #resource}
     */
    public AliasSpecification(Technology technology, Resource resource)
    {
        this.setTechnology(technology);
        this.setResource(resource);
    }

    /**
     * Constructor.
     *
     * @param technology sets the {@link #technology}
     */
    public AliasSpecification(Technology technology)
    {
        this.setTechnology(technology);
    }

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #aliasType}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public AliasType getAliasType()
    {
        return aliasType;
    }

    /**
     * @param aliasType sets the {@link #aliasType}
     */
    public void setAliasType(AliasType aliasType)
    {
        this.aliasType = aliasType;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasSpecification aliasSpecification = (AliasSpecification) specification;

        boolean modified = false;
        modified |= !ObjectUtils.equals(getTechnology(), aliasSpecification.getTechnology())
                || !ObjectUtils.equals(getAliasType(), aliasSpecification.getAliasType())
                || !ObjectUtils.equals(getResource(), aliasSpecification.getResource());

        setTechnology(aliasSpecification.getTechnology());
        setAliasType(aliasSpecification.getAliasType());
        setResource(aliasSpecification.getResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new ReservationTask(context)
        {
            @Override
            protected Reservation createReservation() throws ReportException
            {
                Cache.Transaction cacheTransaction = getCacheTransaction();
                AvailableAlias availableAlias = null;
                // First try to allocate alias from a resource capabilities
                Resource resource = getResource();
                if (resource != null) {
                    List<AliasProviderCapability> aliasProviderCapabilities =
                            resource.getCapabilities(AliasProviderCapability.class);
                    for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
                        availableAlias = getCache().getAvailableAlias(aliasProviderCapability, cacheTransaction,
                                getTechnology(), getAliasType(), getInterval());
                        if (availableAlias != null) {
                            break;
                        }
                    }
                }
                // Allocate alias from all resources in the cache
                if (availableAlias == null) {
                    availableAlias = getCache().getAvailableAlias(
                            cacheTransaction, getTechnology(), getAliasType(), getInterval());
                }
                if (availableAlias == null) {
                    throw new NoAvailableAliasReport(getTechnology(), getAliasType()).exception();
                }

                // Reuse existing reservation
                Alias alias = availableAlias.getAlias();
                if (alias.isPersisted()) {
                    AliasReservation providedAliasReservation = cacheTransaction.getProvidedAliasReservation(alias);
                    if (providedAliasReservation != null) {
                        ExistingReservation existingReservation = new ExistingReservation();
                        existingReservation.setSlot(getInterval());
                        existingReservation.setReservation(providedAliasReservation);
                        cacheTransaction.removeProvidedReservation(providedAliasReservation);
                        return existingReservation;
                    }

                }

                // Create new reservation
                AliasReservation aliasReservation = new AliasReservation();
                aliasReservation.setSlot(getInterval());
                aliasReservation.setAliasProviderCapability(availableAlias.getAliasProviderCapability());
                aliasReservation.setAlias(alias);
                return aliasReservation;
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        aliasSpecificationApi.setTechnology(getTechnology());
        aliasSpecificationApi.setAliasType(getAliasType());
        if (getResource() != null) {
            aliasSpecificationApi.setResourceIdentifier(domain.formatIdentifier(getResource().getId()));
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.TECHNOLOGY)) {
            setTechnology(aliasSpecificationApi.getTechnology());
        }
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.ALIAS_TYPE)) {
            setAliasType(aliasSpecificationApi.getAliasType());
        }
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.RESOURCE_IDENTIFIER)) {
            if (aliasSpecificationApi.getResourceIdentifier() == null) {
                setResource(null);
            }
            else {
                Long resourceId = domain.parseIdentifier(aliasSpecificationApi.getResourceIdentifier());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }
        super.fromApi(specificationApi, entityManager, domain);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technology", technology);
        map.put("aliasType", aliasType);
    }
}
