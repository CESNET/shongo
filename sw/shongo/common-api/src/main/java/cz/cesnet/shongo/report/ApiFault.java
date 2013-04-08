package cz.cesnet.shongo.report;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ApiFault
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
