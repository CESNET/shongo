package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.AbstractStateReport;
import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.Reportable;
import cz.cesnet.shongo.report.SerializableReport;

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
        put(AbstractStateReport.ID, report.getUniqueId());
        for (Map.Entry<String, Object> parameter : report.getParameters().entrySet()) {
            String name = parameter.getKey();
            Object value = parameter.getValue();
            if (value instanceof String) {
                put(name, value);
            }
            else if (value instanceof Collection) {
                Collection collection = (Collection) value;
                if (!collection.isEmpty()) {
                    put(name, collection);
                }
            }
            else if (value instanceof SerializableReport) {
                SerializableReport serializableReport = (SerializableReport) value;
                Map<String, Object> childReport = new StateReportSerializer(serializableReport);
                put(name, childReport);
            }
            else if (value instanceof Reportable) {
                Reportable reportable = (Reportable) value;
                put(name, reportable.getReportDescription(Report.MessageType.DOMAIN_ADMIN));
            }
            else if (value != null) {
                throw new TodoImplementException(value.getClass());
            }
        }
    }
}
