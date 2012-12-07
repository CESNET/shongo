package cz.cesnet.shongo;

/**
 * Enumeration for alias value type.
 */
public enum AliasValueType
{
    /**
     * @see <a href="http://en.wikipedia.org/wiki/E.164">E.164</a>
     */
    E164,

    /**
     * e.g., H.323 ID or Adobe Connect room name
     */
    STRING,

    /**
     * e.g., SIP URI or Adobe Connect url
     */
    URI
}
