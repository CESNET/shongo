package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.List;
import java.util.Set;

/**
 * Represents an entity (or multiple entities) which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpoint extends Endpoint
{
    /**
     * {@link ExternalEndpointSpecification} for the {@link ExternalEndpoint}.
     */
    private ExternalEndpointSpecification externalEndpointSpecification;

    /**
     * Constructor.
     */
    public ExternalEndpoint()
    {
    }

    /**
     * Constructor.
     *
     * @param externalEndpointSpecification sets the {@link #externalEndpointSpecification}
     */
    public ExternalEndpoint(ExternalEndpointSpecification externalEndpointSpecification)
    {
        this.externalEndpointSpecification = externalEndpointSpecification;
    }

    /**
     * @return {@link #externalEndpointSpecification}
     */
    @ManyToOne
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
    @Transient
    public int getCount()
    {
        return externalEndpointSpecification.getCount();
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        return externalEndpointSpecification.getTechnologies();
    }

    @Override
    @Transient
    public void addAlias(Alias alias)
    {
        throw new IllegalStateException("Cannot assign alias to allocated external endpoint.");
    }

    @Override
    @Transient
    public List<Alias> getAliases()
    {
        return externalEndpointSpecification.getAliases();
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return String.format("external endpoint(count: %d)",
                externalEndpointSpecification.getCount());
    }

    @Override
    @Transient
    public CallInitiation getCallInitiation()
    {
        return externalEndpointSpecification.getCallInitiation();
    }
}
