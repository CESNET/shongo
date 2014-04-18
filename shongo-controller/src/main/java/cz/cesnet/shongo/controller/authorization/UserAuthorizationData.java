package cz.cesnet.shongo.controller.authorization;

/**
 * User authorization data.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserAuthorizationData
{
    /**
     * Available {@link #loa}.
     */
    public static final int LOA_NONE = 0;
    public static final int LOA_BASIC = 1;
    public static final int LOA_EXTENDED = 2;
    public static final int LOA_MIN = LOA_NONE;
    public static final int LOA_MAX = LOA_EXTENDED;

    /**
     * Identity provider.
     */
    final private String provider;

    /**
     * Level of authenticity.
     */
    final private int loa;

    /**
     * Constructor.
     *
     * @param loa sets the {@link #loa}
     */
    public UserAuthorizationData(int loa)
    {
        this(null, loa);
    }

    /**
     * Constructor.
     *
     * @param provider sets the {@link #provider}
     * @param loa sets the {@link #loa}
     */
    public UserAuthorizationData(String provider, int loa)
    {
        if (loa < LOA_MIN || loa > LOA_MAX) {
            throw new IllegalArgumentException("LOA must be in range " + LOA_MIN + "-" + LOA_MAX + ".");
        }
        this.provider = provider;
        this.loa = loa;
    }

    /**
     * @return {@link #provider}
     */
    public String getProvider()
    {
        return provider;
    }

    /**
     * @return {@link #loa}
     */
    public int getLoa()
    {
        return loa;
    }
}
