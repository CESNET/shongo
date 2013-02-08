package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingAliasReport extends Report
{
    /**
     * {@link cz.cesnet.shongo.Technology}s for the {@link cz.cesnet.shongo.controller.resource.Alias}.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * {@link cz.cesnet.shongo.AliasType}s for the {@link cz.cesnet.shongo.controller.resource.Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Value for the {@link cz.cesnet.shongo.controller.resource.Alias}.
     */
    private String value;

    /**
     * Constructor.
     */
    public AllocatingAliasReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technologies
     * @param aliasTypes
     */
    public AllocatingAliasReport(Set<Technology> technologies, Set<AliasType> aliasTypes, String value)
    {
        setTechnologies(technologies);
        setAliasTypes(aliasTypes);
        setValue(value);
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
     * @return {@link #aliasTypes}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<AliasType> getAliasTypes()
    {
        return aliasTypes;
    }

    /**
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public void setAliasTypes(Set<AliasType> aliasTypes)
    {
        this.aliasTypes = aliasTypes;
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
     * @param value {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating alias for the following specification:\n"
                + " Technology: %s\n"
                + " Alias Type: %s\n"
                + " Value: %s",
                (technologies.size() > 0 ? Technology.formatTechnologies(technologies) : "Any"),
                (aliasTypes .size() > 0 ? AliasType.formatAliasTypes(aliasTypes ) : "Any"),
                (value != null ? value : "Any"));
    }
}
