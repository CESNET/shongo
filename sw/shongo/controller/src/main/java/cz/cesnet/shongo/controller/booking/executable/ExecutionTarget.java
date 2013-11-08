package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.executor.ExecutionAction;
import cz.cesnet.shongo.controller.executor.ExecutionReport;
import cz.cesnet.shongo.report.ReportableSimple;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a {@link PersistentObject} which can be executed by {@link ExecutionAction}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ExecutionTarget extends PersistentObject implements ReportableSimple, Reporter.ReportContext
{
    @Id
    @SequenceGenerator(name = "executable_id", sequenceName = "executable_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "executable_id")
    @Override
    public Long getId()
    {
        return id;
    }

    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * Number of attempts which were performed by {@link ExecutionAction}
     */
    private int attemptCount;

    /**
     * {@link DateTime} for next attempt of {@link ExecutionAction}. If the value is {@code null},
     * the {@link ExecutionAction} should be performed as soon as possible.
     */
    private DateTime nextAttempt;

    /**
     * List of report for this object.
     */
    private List<ExecutionReport> reports = new LinkedList<ExecutionReport>();
    /**
     * Cached sorted {@link #reports}.
     */
    private List<ExecutionReport> cachedSortedReports;

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    public final void setSlot(Interval slot)
    {
        setSlot(slot.getStart(), slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param slotStart
     * @param slotEnd
     */
    public void setSlot(DateTime slotStart, DateTime slotEnd)
    {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
    }

    /**
     * @return {@link #attemptCount}
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    public int getAttemptCount()
    {
        return attemptCount;
    }

    /**
     * @param attemptCount sets the {@link #attemptCount}
     */
    public void setAttemptCount(int attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    /**
     * @return {@link #nextAttempt}
     */
    @Column
    @Type(type = "DateTime")
    public DateTime getNextAttempt()
    {
        return nextAttempt;
    }

    /**
     * @param nextAttempt sets the {@link #nextAttempt}
     */
    public void setNextAttempt(DateTime nextAttempt)
    {
        this.nextAttempt = nextAttempt;
    }

    /**
     * @return {@link #reports}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "executionTarget", orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<cz.cesnet.shongo.controller.executor.ExecutionReport> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param reports sets the {@link #reports}
     */
    public void setReports(List<cz.cesnet.shongo.controller.executor.ExecutionReport> reports)
    {
        this.reports.clear();
        for (cz.cesnet.shongo.controller.executor.ExecutionReport report : reports) {
            this.reports.add(report);
        }
        cachedSortedReports = null;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(cz.cesnet.shongo.controller.executor.ExecutionReport report)
    {
        // Manage bidirectional association
        if (reports.contains(report) == false) {
            reports.add(report);
            report.setExecutionTarget(this);
        }
        cachedSortedReports = null;
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(cz.cesnet.shongo.controller.executor.ExecutionReport report)
    {
        // Manage bidirectional association
        if (reports.contains(report)) {
            reports.remove(report);
            report.setExecutionTarget(null);
        }
        cachedSortedReports = null;
    }

    /**
     * Remove all {@link cz.cesnet.shongo.controller.executor.ExecutionReport}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
        cachedSortedReports.clear();
    }

    /**
     * @return last added {@link cz.cesnet.shongo.controller.executor.ExecutionReport}
     */
    @Transient
    public cz.cesnet.shongo.controller.executor.ExecutionReport getLastReport()
    {
        return (reports.size() > 0 ? getCachedSortedReports().get(0) : null);
    }

    /**
     * @return number of {@link cz.cesnet.shongo.controller.executor.ExecutionReport}s
     */
    @Transient
    public int getReportCount()
    {
        return reports.size();
    }

    /**
     * @return {@link #cachedSortedReports}
     */
    @Transient
    protected List<cz.cesnet.shongo.controller.executor.ExecutionReport> getCachedSortedReports()
    {
        if (cachedSortedReports == null) {
            cachedSortedReports = new LinkedList<cz.cesnet.shongo.controller.executor.ExecutionReport>();
            cachedSortedReports.addAll(reports);
            Collections.sort(cachedSortedReports, new Comparator<ExecutionReport>()
            {
                @Override
                public int compare(cz.cesnet.shongo.controller.executor.ExecutionReport o1, cz.cesnet.shongo.controller.executor.ExecutionReport o2)
                {
                    return -o1.getDateTime().compareTo(o2.getDateTime());
                }
            });
        }
        return cachedSortedReports;
    }
}
