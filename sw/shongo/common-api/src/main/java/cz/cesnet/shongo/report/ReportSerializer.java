package cz.cesnet.shongo.report;

/**
 * Represents a serializer for {@link Report} parameters.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReportSerializer
{
    /**
     * @param name of parameter
     * @return value of parameter with given {@code name}
     */
    public Object getParameter(String name, Class type);

    /**
     * @param name  of parameter to be set
     * @param value of parameter to be set
     */
    public void setParameter(String name, Object value);
}
