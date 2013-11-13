package cz.cesnet.shongo;

/**
 * Enumeration for alias value type.
 */
public enum AliasValueType
{
    /**
     * @see <a href="http://en.wikipedia.org/wiki/E.164">E.164</a>
     */
    E164(true),

    /**
     * e.g., H.323 ID or Adobe Connect room name
     */
    STRING(false),

    /**
     * e.g., H.323/SIP URI ("<id>@<domain>") or Adobe Connect url
     */
    URI(true),

    /**
     * e.g., "<ip-address> <number>#"
     */
    IP(false);

    /**
     * Constructor.
     *
     * @param callable sets the {@link #callable}
     */
    private AliasValueType(boolean callable)
    {
        this.callable = callable;
    }

    /**
     * Specifies whether the value represents a callable value.
     */
    boolean callable;

    /**
     * @return {@link #callable}
     */
    public boolean isCallable()
    {
        return callable;
    }
}
