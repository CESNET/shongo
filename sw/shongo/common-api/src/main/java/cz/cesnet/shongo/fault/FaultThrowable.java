package cz.cesnet.shongo.fault;

/**
 * {@link Throwable} which represents a {@link Fault}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface FaultThrowable
{
    /**
     * @return {@link Fault}
     */
    public Fault getFault();
}
