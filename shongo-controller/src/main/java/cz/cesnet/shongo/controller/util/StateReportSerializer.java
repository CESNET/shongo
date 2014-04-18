package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractObjectReport;
import cz.cesnet.shongo.controller.api.ExecutionReport;
import cz.cesnet.shongo.report.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializer for {@link SerializableReport}s to {@link Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StateReportSerializer extends HashMap<String, Object>
{
    /**
     * Constructor.
     *
     * @param report to be serialized.
     */
    public StateReportSerializer(Report report)
    {
        put(AbstractObjectReport.ID, report.getUniqueId());
        put(AbstractObjectReport.TYPE, report.getType());
        if (report instanceof cz.cesnet.shongo.controller.executor.ExecutionReport) {
            cz.cesnet.shongo.controller.executor.ExecutionReport executionReport = (cz.cesnet.shongo.controller.executor.ExecutionReport) report;
            put(ExecutionReport.DATE_TIME, executionReport.getDateTime());
        }
        for (Map.Entry<String, Object> parameter : report.getParameters().entrySet()) {
            String name = parameter.getKey();
            Object value = parameter.getValue();
            if (value instanceof String || value instanceof Integer || value instanceof Enum || value instanceof Boolean) {
                put(name, value);
            }
            else if (value instanceof DateTime || value instanceof Interval || value instanceof Period) {
                put(name, value);
            }
            else if (value instanceof Collection) {
                Collection collection = (Collection) value;
                for (Object item : collection) {
                    if (item instanceof Collection) {
                        ((Collection) item).size();
                    }
                }
                if (!collection.isEmpty()) {
                    put(name, collection);
                }
            }
            else if (value instanceof Map) {
                Map map = (Map) value;
                if (!map.isEmpty()) {
                    put(name, map);
                }
            }
            else if (value instanceof Report) {
                Report reportValue = (Report) value;
                Map<String, Object> childReport = new StateReportSerializer(reportValue);
                put(name, childReport);
            }
            else if (value instanceof ReportableSimple) {
                ReportableSimple reportable = (ReportableSimple) value;
                put(name, reportable.getReportDescription());
            }
            else if (value instanceof ReportableComplex) {
                ReportableComplex reportable = (ReportableComplex) value;
                put(name, reportable.getReportDescription());
            }
            else if (value != null) {
                throw new TodoImplementException(value.getClass());
            }
        }
    }
}
