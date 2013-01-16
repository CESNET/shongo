package cz.cesnet.shongo.controller.resource.value;

/**
 * Object used for generating not-used {@link cz.cesnet.shongo.controller.resource.Alias}es.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ValueGenerator
{
    /**
     * Constructor.
     */
    public ValueGenerator()
    {
    }

    /**
     * Add already used {@code alias} which should not be generated.
     *
     * @param alias which is already used
     */
    public abstract void addValue(String alias);

    /**
     * @return new generated {@link cz.cesnet.shongo.controller.resource.Alias}
     */
    public abstract String generateValue();

    /**
     * @param value to be checked for availability
     * @return true if given {@code value} is available, false otherwise
     */
    public abstract boolean isValueAvailable(String value);
}
