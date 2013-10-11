package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entity (or multiple entities) which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpointSet extends Endpoint
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
    public ExternalEndpointSet()
    {
    }

    /**
     * Constructor.
     *
     * @param externalEndpointSetParticipant
     *         to initialize from
     */
    public ExternalEndpointSet(ExternalEndpointSetParticipant externalEndpointSetParticipant)
    {
        setCount(externalEndpointSetParticipant.getCount());
        for (Technology technology : externalEndpointSetParticipant.getTechnologies()) {
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
    public void addAssignedAlias(Alias alias) throws SchedulerException
    {
        throw new SchedulerReportSet.CompartmentAssignAliasToExternalEndpointException();
    }

    @Override
    @Transient
    public String getDescription()
    {
        return String.format("external endpoint(%dx %s)", count, Technology.formatTechnologies(technologies));
    }
}
