package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a report for a state.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractStateReport extends AbstractComplexType
{
    public static final String ID = "id";
    public static final String CHILDREN = "children";

    /**
     * List of reports. Each report is represented by a map with required {@link #ID} key and optional
     * {@link #CHILDREN} keys.
     */
    private List<Map<String, Object>> reports = new LinkedList<Map<String, Object>>();

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(Map<String, Object> report)
    {
        reports.add(report);
    }

    /**
     * @param locale
     * @return {@link AbstractStateReport} as string for given {@code locale}
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

    private static final String REPORTS = "reports";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(REPORTS, reports);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        for (Object report : dataMap.getList(REPORTS, Object.class)) {
            reports.add(Converter.convertToMap(report, String.class, Object.class));
        }
    }
}
