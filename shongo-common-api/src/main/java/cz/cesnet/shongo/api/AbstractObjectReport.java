package cz.cesnet.shongo.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTimeZone;

import java.util.*;

/**
 * Represents a report for an object.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractObjectReport extends AbstractComplexType
{
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String CHILDREN = "children";

    /**
     * @see Report.UserType
     */
    private Report.UserType userType;

    /**
     * List of reports. Each report is represented by a map with required {@link #ID} key and optional
     * {@link #CHILDREN} keys.
     */
    protected List<Map<String, Object>> reports = new LinkedList<Map<String, Object>>();

    /**
     * Constructor.
     *
     * @param userType sets the {@link #userType}
     */
    public AbstractObjectReport(Report.UserType userType)
    {
        this.userType = userType;
    }

    /**
     * @return {@link #userType}
     */
    public Report.UserType getUserType()
    {
        return userType;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(Map<String, Object> report)
    {
        reports.add(report);
    }

    /**
     * @return last report from {@link #reports}
     */
    public Map<String, Object> getLastReport()
    {
        if (reports.isEmpty()) {
            return null;
        }
        return reports.get(reports.size() - 1);
    }

    /**
     * @return {@link #reports}
     */
    public List<Map<String, Object>> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param locale
     * @param timeZone
     * @return {@link AbstractObjectReport} as string for given {@code locale} and {@code timeZone}
     */
    public String toString(Locale locale, DateTimeZone timeZone)
    {
        throw new TodoImplementException();
    }

    /**
     * @param locale
     * @return {@link AbstractObjectReport} as string for given {@code locale}
     */
    public final String toString(Locale locale)
    {
        return toString(locale, DateTimeZone.getDefault());
    }

    @Override
    public final String toString()
    {
        return toString(Locale.getDefault(), DateTimeZone.getDefault());
    }

    private static final String USER_TYPE = "userType";
    private static final String REPORTS = "reports";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_TYPE, userType);
        dataMap.set(REPORTS, reports);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userType = dataMap.getEnum(USER_TYPE, Report.UserType.class);
        for (Object report : dataMap.getList(REPORTS, Object.class)) {
            reports.add(Converter.convertToMap(report, String.class, Object.class));
        }
    }

    /**
     * @param report
     * @return type of given {@code report}
     */
    protected static Report.Type getReportType(Map<String, Object> report)
    {
        Object value = report.get(TYPE);
        if (value == null) {
            return null;
        }
        else if (value instanceof Report.Type) {
            return (Report.Type) value;
        }
        else {
            return Report.Type.valueOf(value.toString());
        }
    }

    /**
     * @param report
     * @return list of children from given {@code report}
     */
    protected static Collection<Map<String, Object>> getReportChildren(Map<String, Object> report)
    {
        Object children = report.get(CHILDREN);
        if (children == null) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> childMaps = (List) Converter.convertToList(children, Map.class);
        return childMaps;
    }
}
