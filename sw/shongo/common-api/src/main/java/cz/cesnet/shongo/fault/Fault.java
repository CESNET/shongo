package cz.cesnet.shongo.fault;

/**
 * Represents a error state.
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
}
