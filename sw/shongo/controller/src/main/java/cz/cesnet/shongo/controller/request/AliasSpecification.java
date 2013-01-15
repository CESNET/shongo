package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.AliasReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
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
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link AliasProviderCapability} from which the {@link Alias} should be allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

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
     * @param technology              sets the {@link #technology}
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public AliasSpecification(Technology technology, AliasProviderCapability aliasProviderCapability)
    {
        this.setTechnology(technology);
        this.setAliasProviderCapability(aliasProviderCapability);
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
     * @return {@link #value}
     */
    @Column
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #aliasProviderCapability}
     */
    @OneToOne
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasSpecification aliasSpecification = (AliasSpecification) specification;

        boolean modified = false;
        modified |= !ObjectUtils.equals(getTechnology(), aliasSpecification.getTechnology())
                || !ObjectUtils.equals(getAliasType(), aliasSpecification.getAliasType())
                || !ObjectUtils.equals(getAliasProviderCapability(), aliasSpecification.getAliasProviderCapability());

        setTechnology(aliasSpecification.getTechnology());
        setAliasType(aliasSpecification.getAliasType());
        setAliasProviderCapability(aliasSpecification.getAliasProviderCapability());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        AliasReservationTask aliasReservationTask = new AliasReservationTask(context);
        if (technology != null) {
            aliasReservationTask.addTechnology(technology);
        }
        aliasReservationTask.setAliasType(aliasType);
        aliasReservationTask.setValue(value);
        if (aliasProviderCapability != null) {
            aliasReservationTask.addAliasProviderCapability(aliasProviderCapability);
        }
        return aliasReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        aliasSpecificationApi.setTechnology(getTechnology());
        aliasSpecificationApi.setAliasType(getAliasType());
        aliasSpecificationApi.setValue(getValue());
        if (getAliasProviderCapability() != null) {
            aliasSpecificationApi.setResourceId(
                    Domain.getLocalDomain().formatId(getAliasProviderCapability().getResource()));
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
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
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.VALUE)) {
            setValue(aliasSpecificationApi.getValue());
        }
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.RESOURCE_ID)) {
            if (aliasSpecificationApi.getResourceId() == null) {
                setAliasProviderCapability(null);
            }
            else {
                Long resourceId = Domain.getLocalDomain().parseId(aliasSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                Resource resource = resourceManager.get(resourceId);
                AliasProviderCapability aliasProviderCapability = resource.getCapability(AliasProviderCapability.class);
                if (aliasProviderCapability == null) {
                    throw new FaultException("Resource '%s' doesn't have %s.",
                            AliasProviderCapability.class.getSimpleName(), aliasSpecificationApi.getResourceId());
                }
                setAliasProviderCapability(aliasProviderCapability);
            }
        }
        super.fromApi(specificationApi, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technology", technology);
        map.put("aliasType", aliasType);
    }
}
