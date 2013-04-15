package cz.cesnet.shongo.report;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a set/group of {@link Report}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReportSet
{
    /**
     * List of {@link Report} classes for this {@link AbstractReportSet}.
     */
    private List<Class<? extends Report>> reportClasses;

    /**
     * @param reportClass to be added to the {@link #reportClasses}
     */
    protected final void addReportClass(Class<? extends Report> reportClass)
    {
        reportClasses.add(reportClass);
    }

    /**
     * @return {@link #reportClasses}
     */
    public final Collection<Class<? extends Report>> getReportClasses()
    {
        if (reportClasses == null) {
            reportClasses = new LinkedList<Class<? extends Report>>();
            fillReportClasses();
        }
        return Collections.unmodifiableCollection(reportClasses);
    }

    /**
     * Add all {@link Report} classes to this {@link AbstractReportSet}.
     */
    protected abstract void fillReportClasses();
}
