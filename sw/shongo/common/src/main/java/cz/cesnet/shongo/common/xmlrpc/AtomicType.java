package cz.cesnet.shongo.common.xmlrpc;

/**
 * Represents a type that can be serialized
 * to XML-RPC string type.
 *
 * @author Martin Srom
 */
public interface AtomicType
{
    /**
     * Load atomic type from string
     *
     * @param string
     */
    public void fromString(String string);
}
