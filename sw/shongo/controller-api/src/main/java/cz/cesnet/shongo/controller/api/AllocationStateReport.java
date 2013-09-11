package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;

import java.util.Locale;

/**
 * {@link AbstractStateReport} for {@link AllocationState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReport extends AbstractStateReport
{
    @Override
    public String toString(Locale locale)
    {
        /*StringBuilder stringBuilder = new StringBuilder();
        for (SchedulerReport report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append(report.getMessageRecursive(messageType));
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);*/
        throw new TodoImplementException();
    }
}
