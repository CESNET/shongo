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
     * Value for the {@link Alias}.
     */
    private String value;

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
    public NoAvailableAliasReport(Set<Technology> technologies, AliasType aliasType, String value)
    {
        setTechnologies(technologies);
        setAliasType(aliasType);
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
        return String.format("No available alias was found for the following specification:\n"
                + " Technology: %s\n"
                + " Alias Type: %s\n"
                + " Value: %s",
                (technologies.size() > 0 ? Technology.formatTechnologies(technologies) : "Any"),
                (aliasType != null ? aliasType.toString() : "Any"),
                (value != null ? value : "Any"));
    }
}
