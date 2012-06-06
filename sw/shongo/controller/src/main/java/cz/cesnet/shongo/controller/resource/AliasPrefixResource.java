package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Represents a special type of {@link AliasResource} which
 * can be allocated as aliases from a single prefix.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasPrefixResource extends AliasResource
{
    /**
     * Technology of aliases.
     */
    private Technology technology;

    /**
     * Type of aliases.
     */
    private Type type;

    /**
     * Prefix of aliases.
     */
    private String prefix;

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
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #prefix}
     */
    @Column
    public String getPrefix()
    {
        return prefix;
    }

    /**
     * @param prefix sets the {@link #prefix}
     */
    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }
}
