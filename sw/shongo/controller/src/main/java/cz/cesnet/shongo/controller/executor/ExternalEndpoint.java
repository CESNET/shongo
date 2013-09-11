package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.controller.scheduler.SchedulerReportSet;
import cz.cesnet.shongo.report.AbstractReport;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an entity (or multiple entities) which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExternalEndpoint extends Endpoint
{
    /**
     * Set of technologies for external endpoints.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * List of aliases that can be used to reference the external endpoint.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    private List<Alias> aliases = new ArrayList<Alias>();

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
        for (Technology technology : externalEndpointSpecification.getTechnologies()) {
            addTechnology(technology);
        }
        for (Alias alias : externalEndpointSpecification.getAliases()) {
            addAlias(alias.clone());
        }
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

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        aliases.addAll(this.aliases);
        aliases.addAll(super.getAssignedAliases());
        return aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    @Override
    @Transient
    public void addAssignedAlias(Alias alias) throws SchedulerException
    {
        throw new SchedulerReportSet.CompartmentAssignAliasToExternalEndpointException();
    }

    @Override
    @Transient
    public String getReportDescription(AbstractReport.MessageType messageType)
    {
        return String.format("external endpoint(%s)", Technology.formatTechnologies(technologies));
    }
}
