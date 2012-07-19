package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.api.AliasType;
import cz.cesnet.shongo.api.Technology;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Represents a resource that can be allocated as an alias(es).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AliasResource extends Resource
{
    /**
     * Technology of aliases.
     */
    private Technology technology;

    /**
     * Type of aliases.
     */
    private AliasType type;

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
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public AliasType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(AliasType type)
    {
        this.type = type;
    }
}
