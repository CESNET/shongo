package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;

/**
 * Object used for generating not-used {@link Alias}es.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AliasGenerator
{
    /**
     * Technology of aliases.
     */
    protected Technology technology;

    /**
     * Type of aliases.
     */
    protected AliasType type;

    /**
     * Contructor.
     *
     * @param technology sets the {@link #technology}
     * @param type       sets the {@link #type}
     */
    public AliasGenerator(Technology technology, AliasType type)
    {
        this.technology = technology;
        this.type = type;
    }

    /**
     * Add already used {@code alias} which should not be generated.
     *
     * @param alias which is already used
     */
    public abstract void addAlias(Alias alias);

    /**
     * @return new generated {@link Alias}
     */
    public abstract Alias generate();
}
