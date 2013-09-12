package cz.cesnet.shongo.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.Report;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a report for an entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractEntityReport extends AbstractComplexType
{
    public static final String ID = "id";
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
    public AbstractEntityReport(Report.UserType userType)
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
     * @param locale
     * @return {@link AbstractEntityReport} as string for given {@code locale}
     */
    public String toString(Locale locale)
    {
        throw new TodoImplementException();
    }

    @Override
    public String toString()
    {
        return toString(Locale.getDefault());
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
}
