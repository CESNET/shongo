package cz.cesnet.shongo.api;

/**
 * Enumeration for alias type.
 */
public enum AliasType
{
    /**
     * @see <a href="http://en.wikipedia.org/wiki/E.164">E.164</a>
     */
    E164,

    /**
     * e.g., H.323 ID
     */
    IDENTIFIER,

    /**
     * e.g., H.323 or SIP URI
     */
    URI
}
