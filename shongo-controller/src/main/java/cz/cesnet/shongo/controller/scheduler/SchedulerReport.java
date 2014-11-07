package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.api.AllocationStateReport;
import cz.cesnet.shongo.controller.util.StateReportSerializer;
import cz.cesnet.shongo.report.AbstractReport;

import javax.persistence.*;
import java.util.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class SchedulerReport extends AbstractReport
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

    /**
     * Parent {@link cz.cesnet.shongo.report.AbstractReport} to which it belongs.
     */
    private SchedulerReport parentReport;

    /**
     * List of child resources (e.g., physical room can contain some videoconferencing equipment).
     */
    private List<SchedulerReport> childReports = new ArrayList<SchedulerReport>();

    /**
     * Constructor.
     */
    public SchedulerReport()
    {
    }

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return {@link #parentReport}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public SchedulerReport getParentReport()
    {
        return parentReport;
    }

    /**
     * @return true if {@link #parentReport} is not null,
     *         false otherwise
     */
    public boolean hasParentReport()
    {
        return parentReport != null;
    }

    /**
     * @param parentReport sets the {@link #parentReport}
     */
    public void setParentReport(SchedulerReport parentReport)
    {
        // Manage bidirectional association
        if (parentReport != this.parentReport) {
            if (this.parentReport != null) {
                SchedulerReport oldParentMessage = this.parentReport;
                this.parentReport = null;
                oldParentMessage.removeChildReport(this);
            }
            if (parentReport != null) {
                this.parentReport = parentReport;
                this.parentReport.addChildReport(this);
            }
        }
    }

    /**
     * @return {@link #childReports}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentReport")
    @Access(AccessType.FIELD)
    public List<SchedulerReport> getChildReports()
    {
        return childReports;
    }

    /**
     * @param childReports sets the {@link #childReports}
     */
    public void setChildReports(List<SchedulerReport> childReports)
    {
        this.childReports = childReports;
    }

    /**
     * @param report to be added to the {@link #childReports}
     */
    public SchedulerReport addChildReport(SchedulerReport report)
    {
        // Manage bidirectional association
        if (childReports.contains(report) == false) {
            childReports.add(report);
            report.setParentReport(this);
        }
        return report;
    }

    /**
     * @param report    to be searched in the {@link #childReports} and removed
     * @param replaceBy to be added to the {@link #childReports} at the index of {@code report}
     */
    public void replaceChildReport(SchedulerReport report, SchedulerReport replaceBy)
    {
        for (int index = 0; index < childReports.size(); index++) {
            if (childReports.get(index).equals(report)) {
                childReports.remove(index);
                report.setParentReport(null);
                childReports.add(index, replaceBy);
                replaceBy.setParentReport(this);
                break;
            }
        }
    }

    /**
     * @param reports to be added to the {@link #childReports}
     */
    public void addChildReports(Collection<SchedulerReport> reports)
    {
        for (SchedulerReport report : reports) {
            addChildReport(report);
        }
    }

    /**
     * @param message to be removed from the {@link #childReports}
     */
    public void removeChildReport(SchedulerReport message)
    {
        // Manage bidirectional association
        if (childReports.contains(message)) {
            childReports.remove(message);
            message.setParentReport(null);
        }
    }

    /**
     * @param userType
     * @return true if the {@link SchedulerReport} is visible in message of given {@code userType},
     *         false otherwise
     */
    public boolean isVisible(UserType userType)
    {
        return !userType.equals(UserType.USER) || isVisible(VISIBLE_TO_USER);
    }

    @Override
    public String toString()
    {
        return toAllocationStateReport(UserType.DOMAIN_ADMIN).toString();
    }

    /**
     * @param userType
     * @return this {@link SchedulerReport} as {@link AllocationStateReport} for given {@code userType}
     */
    public AllocationStateReport toAllocationStateReport(UserType userType)
    {
        List<Map<String, Object>> reports = new LinkedList<Map<String, Object>>();
        toMap(reports, userType);

        AllocationStateReport allocationStateReport = new AllocationStateReport(userType);
        for (Map<String, Object> report : reports) {
            allocationStateReport.addReport(report);
        }
        return allocationStateReport;
    }

    /**
     * @param previousReport
     * @return this {@link SchedulerReport} as {@link AllocationStateReport} appended for given {@code previousReport}
     */
    public AllocationStateReport toAllocationStateReport(AllocationStateReport previousReport)
    {
        List<Map<String, Object>> reports = new LinkedList<Map<String, Object>>();
        toMap(reports, previousReport.getUserType());

        AllocationStateReport allocationStateReport = previousReport;
        for (Map<String, Object> report : reports) {
            allocationStateReport.addReport(report);
        }
        return allocationStateReport;
    }

    /**
     * @param reports     to be filled by reports as maps
     * @param userType to be used
     */
    private void toMap(List<Map<String, Object>> reports, UserType userType)
    {
        Map<String, Object> report = null;
        List<Map<String, Object>> childReports;
        if (isVisible(userType)) {
            report = new StateReportSerializer(this);
            reports.add(report);
            childReports = new LinkedList<Map<String, Object>>();
        }
        else {
            childReports = reports;
        }
        if (this.childReports.size() > 0) {
            for (SchedulerReport childReport : this.childReports) {
                childReport.toMap(childReports, userType);
            }
            if (report != null && childReports.size() > 0) {
                report.put(AllocationStateReport.CHILDREN, childReports);
            }
        }
    }

    /**
     * @param reports     to be used
     * @param userType to be used
     * @return given {@code reports} as {@link AllocationStateReport} for given {@code userType}
     */
    public static AllocationStateReport getAllocationStateReport(List<SchedulerReport> reports, UserType userType)
    {
        List<Map<String, Object>> reportMaps = new LinkedList<Map<String, Object>>();
        for (SchedulerReport report : reports) {
            report.toMap(reportMaps, userType);
        }

        AllocationStateReport allocationStateReport = new AllocationStateReport(userType);
        for (Map<String, Object> report : reportMaps) {
            allocationStateReport.addReport(report);
        }
        return allocationStateReport;
    }
}
