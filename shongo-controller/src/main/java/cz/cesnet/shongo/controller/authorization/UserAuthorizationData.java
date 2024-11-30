package cz.cesnet.shongo.controller.authorization;

import org.joda.time.DateTime;

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
    private final String provider;

    /**
     * Instant
     */
    private final DateTime instant;

    /**
     * Level of authenticity.
     */
    private final int loa;

    /**
     * Constructor.
     *
     * @param loa sets the {@link #loa}
     */
    public UserAuthorizationData(int loa)
    {
        this(null, null, loa);
    }

    /**
     * Constructor.
     *
     * @param provider sets the {@link #provider}
     * @param loa sets the {@link #loa}
     */
    public UserAuthorizationData(String provider, DateTime instant, int loa)
    {
        if (loa < LOA_MIN || loa > LOA_MAX) {
            throw new IllegalArgumentException("LOA must be in range " + LOA_MIN + "-" + LOA_MAX + ".");
        }
        this.provider = provider;
        this.instant = instant;
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
     * @return {@link #instant}
     */
    public DateTime getInstant()
    {
        return instant;
    }

    /**
     * @return {@link #loa}
     */
    public int getLoa()
    {
        return loa;
    }

    public static int getLoaFromDate(DateTime dateTime) {
        if (dateTime.isAfter(DateTime.now().minusYears(1))) {
            return LOA_EXTENDED;
        }
        if (dateTime.isAfter(DateTime.now().minusYears(2))) {
            return LOA_BASIC;
        }
        return LOA_NONE;
    }
}
