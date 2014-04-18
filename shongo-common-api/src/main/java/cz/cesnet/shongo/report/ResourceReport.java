package cz.cesnet.shongo.report;

/**
 * Can be implemented by {@link AbstractReport}s which have resource-id as parameter.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ResourceReport
{
    /**
     * @return resource-id which is referenced by {@link AbstractReport} parameters
     */
    public String getResourceId();
}
