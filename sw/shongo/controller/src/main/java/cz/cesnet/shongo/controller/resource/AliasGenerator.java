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
     * Constructor.
     */
    public AliasGenerator()
    {
    }

    /**
     * Add already used {@code alias} which should not be generated.
     *
     * @param alias which is already used
     */
    public abstract void addAliasValue(String alias);

    /**
     * @return new generated {@link Alias}
     */
    public abstract String generateValue();

    /**
     * @param value to be checked for availability
     * @return true if given {@code value} is available, false otherwise
     */
    public abstract boolean isValueAvailable(String value);
}
