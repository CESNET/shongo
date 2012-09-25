package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class NoAvailableAliasReport extends Report
{
    /**
     *{@link Technology} for the {@link Alias}.
     */
    private Technology technology;

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
     * @param technology
     */
    public NoAvailableAliasReport(Technology technology, AliasType aliasType)
    {
        setTechnology(technology);
        setAliasType(aliasType);
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

    @Override
    @Transient
    public String getText()
    {
        return String.format("No available alias was found for the following specification:\n"
                + " Technology: %s\n"
                + " Alias Type: %s", technology.getName(), (aliasType != null ? aliasType.toString() : "Any"));
    }
}
