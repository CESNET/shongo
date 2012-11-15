package cz.cesnet.shongo.api.xmlrpc;

/**
 * Represents a type that can be serialized
 * from/to {@link String} type.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AtomicType
{
    /**
     * Load atomic type from {@link String}
     *
     * @param string
     */
    public void fromString(String string);
}
