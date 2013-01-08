package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class NoAvailableAliasReport extends Report
{
    /**
     * {@link Technology}s for the {@link Alias}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * {@link AliasType} for the {@link Alias}.
     */
    private AliasType aliasType;

    /**
     * Constructor.
     */
    public NoAvailableAliasReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technologies
     * @param aliasType
     */
    public NoAvailableAliasReport(Set<Technology> technologies, AliasType aliasType)
    {
        setTechnologies(technologies);
        setAliasType(aliasType);
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
        this.technologies.clear();
        this.technologies.addAll(technologies);
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

    @Override
    @Transient
    public String getText()
    {
        return String.format("No available alias was found for the following specification:\n"
                + " Technology: %s\n"
                + " Alias Type: %s",
                Technology.formatTechnologies(technologies), (aliasType != null ? aliasType.toString() : "Any"));
    }
}
