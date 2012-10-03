package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
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
     * Number of external endpoints of the same type.
     */
    private int count = 1;

    /**
     * Set of technologies for external endpoints.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public ExternalEndpoint()
    {
    }

    /**
     * Constructor.
     *
     * @param externalEndpointSpecification to initialize from
     */
    public ExternalEndpoint(ExternalEndpointSpecification externalEndpointSpecification)
    {
        this.count = externalEndpointSpecification.getCount();
        for (Technology technology : externalEndpointSpecification.getTechnologies()) {
            addTechnology(technology);
        }
    }

    /**
     * @return {@link #count}
     */
    @Column(name = "same_count")
    public int getCount()
    {
        return count;
    }

    /**
     * @param count sets the {@link #count}
     */
    public void setCount(int count)
    {
        this.count = count;
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    @Override
    @Transient
    public void addAssignedAlias(Alias alias)
    {
        throw new IllegalStateException("Cannot assign alias to allocated external endpoint.");
    }

    @Override
    @Transient
    public String getReportDescription()
    {
        return String.format("external endpoint(%dx %s)", count, Technology.formatTechnologies(technologies));
    }
}
