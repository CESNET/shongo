package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.ExecutionReport;
import cz.cesnet.shongo.controller.executor.ExecutionAction;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportableSimple;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import jakarta.persistence.*;
import java.util.*;

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
     * Specifies when the {@link ExecutionTarget} becomes valid for execution.
     */
    private DateTime slotStart;

    /**
     * Specifies when the {@link ExecutionTarget} become invalid fro execution.
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
    private List<cz.cesnet.shongo.controller.executor.ExecutionReport> reports = new LinkedList<cz.cesnet.shongo.controller.executor.ExecutionReport>();
    /**
     * Cached sorted {@link #reports}.
     */
    private List<cz.cesnet.shongo.controller.executor.ExecutionReport> cachedSortedReports;

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(PersistentDateTime.class)
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
    @Type(PersistentDateTime.class)
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
    @Type(PersistentDateTime.class)
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

    @PrePersist
    @PreUpdate
    protected void onUpdate()
    {
        if (slotStart.isAfter(slotEnd)) {
            throw new RuntimeException("Slot start can't be after slot end.");
        }
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
            Collections.sort(cachedSortedReports, new Comparator<cz.cesnet.shongo.controller.executor.ExecutionReport>()
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

    /**
     * @return formatted {@link #reports} as string
     */
    @Transient
    protected ExecutionReport getExecutionReport(Report.UserType userType)
    {
        ExecutionReport executionReport = new ExecutionReport(userType);
        for (cz.cesnet.shongo.controller.executor.ExecutionReport report : getCachedSortedReports()) {
            executionReport.addReport(new StateReportSerializer(report));
        }
        return executionReport;
    }

    /**
     * @return collection of {@link ExecutionTarget} dependencies
     */
    @Transient
    public Collection<? extends ExecutionTarget> getExecutionDependencies()
    {
        return Collections.emptyList();
    }
}
