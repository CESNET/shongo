package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents an allocated {@link cz.cesnet.shongo.controller.resource.Resource} in an {@link cz.cesnet.shongo.controller.allocation.AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedExternalEndpoint extends AllocatedItem implements AllocatedEndpoint
{
    /**
     * {@link ExternalEndpointSpecification} which is allocated.
     */
    private ExternalEndpointSpecification externalEndpointSpecification;

    /**
     * Constructor.
     */
    public AllocatedExternalEndpoint()
    {
    }

    /**
     * Constructor.
     *
     * @param externalEndpointSpecification sets the {@link #externalEndpointSpecification}
     */
    public AllocatedExternalEndpoint(ExternalEndpointSpecification externalEndpointSpecification)
    {
        this.externalEndpointSpecification = externalEndpointSpecification;
    }

    /**
     * @return {@link #externalEndpointSpecification}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public ExternalEndpointSpecification getExternalEndpointSpecification()
    {
        return externalEndpointSpecification;
    }

    /**
     * @param externalEndpointSpecification sets the {@link #externalEndpointSpecification}
     */
    public void setExternalEndpointSpecification(ExternalEndpointSpecification externalEndpointSpecification)
    {
        this.externalEndpointSpecification = externalEndpointSpecification;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AllocatedItem createApi()
    {
        return null;
    }

    @Override
    @Transient
    public int getCount()
    {
        return externalEndpointSpecification.getCount();
    }

    @Override
    @Transient
    public Set<Technology> getSupportedTechnologies()
    {
        return externalEndpointSpecification.getTechnologies();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return false;
    }

    @Override
    public void assignAlias(Alias alias)
    {
        throw new IllegalStateException("Cannot assign alias to allocated external endpoint.");
    }

    @Override
    @Transient
    public List<Alias> getAssignedAliases()
    {
        return externalEndpointSpecification.getAliases();
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return null;
    }
}
