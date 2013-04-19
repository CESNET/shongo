package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a set of technologies which can be persisted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class TechnologySet extends PersistentObject
{
    /**
     * Set of technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public TechnologySet()
    {
    }

    /**
     * Constructor.
     *
     * @param technologies sets the {@link #technologies}
     */
    public TechnologySet(Set<Technology> technologies)
    {
        setTechnologies(technologies);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    @Override
    public String toString()
    {
        return Technology.formatTechnologies(technologies);
    }
}
