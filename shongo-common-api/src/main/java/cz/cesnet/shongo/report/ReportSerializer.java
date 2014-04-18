package cz.cesnet.shongo.report;

/**
 * Represents a serializer for {@link AbstractReport} parameters.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ReportSerializer
{
    /**
     *
     * @param name of parameter
     * @param elementTypes
     * @return value of parameter with given {@code name}
     */
    public Object getParameter(String name, Class type, Class... elementTypes);

    /**
     * @param name  of parameter to be set
     * @param value of parameter to be set
     */
    public void setParameter(String name, Object value);
}
