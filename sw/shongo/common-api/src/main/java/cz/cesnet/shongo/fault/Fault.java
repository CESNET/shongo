package cz.cesnet.shongo.fault;

/**
 * Represents an API fault.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Fault
{
    /**
     * @return fault code
     */
    public int getCode();

    /**
     * @return fault message
     */
    public String getMessage();

    /**
     * @return {@link Exception}
     */
    public Exception createException();
}
