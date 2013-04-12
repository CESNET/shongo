package cz.cesnet.shongo.report;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReportSet
{
    private List<Class<? extends Report>> reportClasses;

    protected final void addReportClass(Class<? extends Report> reportClass)
    {
        reportClasses.add(reportClass);
    }

    public Collection<Class<? extends Report>> getReportClasses()
    {
        if (reportClasses == null) {
            reportClasses = new LinkedList<Class<? extends Report>>();
            fillReportClasses();
        }
        return Collections.unmodifiableCollection(reportClasses);
    }

    protected abstract void fillReportClasses();
}
