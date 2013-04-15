package cz.cesnet.shongo.report;

/**
 * {@link Exception} which implements the {@link ApiFaultException} represents a XML-RPC fault exception.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ApiFaultException
{
    /**
     * @return {@link ApiFault}
     */
    public ApiFault getApiFault();
}
