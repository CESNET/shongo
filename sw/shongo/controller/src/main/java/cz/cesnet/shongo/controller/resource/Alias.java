package cz.cesnet.shongo.controller.resource;

/**
 * Represents an alias for a single device and a single technology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Alias
{
    /**
     * Enumeration for alias type.
     */
    public static enum Type
    {
        /**
         * @see <a href="http://en.wikipedia.org/wiki/E.164">E.164</a>
         */
        E164,

        /**
         * e.g., H.323 ID
         */
        Identifier,

        /**
         * e.g., H.323 or SIP URI
         */
        URI
    }

    /**
     * @return technology of alias
     */
    public Technology getTechnology();

    /**
     * @return type of alias
     */
    public Type getType();

    /**
     * @return value of alias
     */
    public String getValue();
}
