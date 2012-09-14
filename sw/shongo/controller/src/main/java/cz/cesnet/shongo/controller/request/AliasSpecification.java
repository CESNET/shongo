package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.AliasReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.Map;

/**
 * Represents a {@link cz.cesnet.shongo.controller.request.Specification} for a person.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSpecification extends Specification
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
    public AliasReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new AliasReservationTask(this, context);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technology", technology);
        map.put("aliasType", aliasType);
    }
}
