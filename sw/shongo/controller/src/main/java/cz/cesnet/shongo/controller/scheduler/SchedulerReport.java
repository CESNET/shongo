package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.api.AllocationStateReport;
import cz.cesnet.shongo.controller.util.MapReportSerializer;
import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.SerializableReport;

import javax.persistence.*;
import java.util.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class SchedulerReport extends AbstractReport implements SerializableReport
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
     * @return formatted text and help of the {@link cz.cesnet.shongo.report.AbstractReport}
     */
    @Transient
    public String getMessageRecursive(MessageType messageType)
    {
        // Get child reports
        List<SchedulerReport> childReports = new LinkedList<SchedulerReport>();
        getMessageRecursiveChildren(messageType, childReports);

        StringBuilder messageBuilder = new StringBuilder();
        String message = null;
        if (isVisible(messageType)) {
            // Append prefix
            message = getMessage(messageType);
            messageBuilder.append("-");
            switch (getType()) {
                case ERROR:
                    messageBuilder.append("[ERROR] ");
                    break;
                default:
                    break;
            }

            // Append message
            if (childReports.size() > 0) {
                message = message.replace("\n", String.format("\n  |%" + (messageBuilder.length() - 3) + "s", ""));
            }
            else {
                message = message.replace("\n", String.format("\n%" + messageBuilder.length() + "s", ""));
            }
            messageBuilder.append(message);

            // Append child reports
            int childReportsCount = childReports.size();
            for (int index = 0; index < childReportsCount; index++) {
                String childReportString = childReports.get(index).getMessageRecursive(messageType);
                if (childReportString != null) {
                    messageBuilder.append("\n  |");
                    messageBuilder.append("\n  +-");
                    childReportString = childReportString.replace("\n",
                            (index < (childReportsCount - 1) ? "\n  | " : "\n    "));
                    messageBuilder.append(childReportString);
                }
            }
        }
        else {
            for (SchedulerReport childReport : childReports) {
                if (messageBuilder.length() > 0) {
                    messageBuilder.append("\n\n");
                }
                messageBuilder.append(childReport.getMessageRecursive(messageType));
            }

        }
        return (messageBuilder.length() > 0 ? messageBuilder.toString() : null);
    }

    /**
     * Add all visible child {@link SchedulerReport}s to given {@code childReports}.
     *
     * @param messageType for the visibility check
     * @param childReports where all child reports should be added
     */
    public void getMessageRecursiveChildren(MessageType messageType, Collection<SchedulerReport> childReports)
    {
        for (SchedulerReport childReport : this.childReports) {
            if (childReport.isVisible(messageType)) {
                childReports.add(childReport);
            }
            else {
                childReport.getMessageRecursiveChildren(messageType, childReports);
            }
        }
    }

    /**
     * @param messageType
     * @return true if the {@link SchedulerReport} is visible in message of given {@code messageType},
     *         false otherwise
     */
    public boolean isVisible(MessageType messageType)
    {
        return !messageType.equals(MessageType.USER) || isVisible(VISIBLE_TO_USER);
    }

    @Override
    public String toString()
    {
        return toAllocationStateReport(MessageType.DOMAIN_ADMIN).toString();
    }

    /**
     * @param messageType
     * @return this {@link SchedulerReport} as {@link AllocationStateReport} for given {@code messageType}
     */
    public AllocationStateReport toAllocationStateReport(MessageType messageType)
    {
        List<Map<String, Object>> reports = new LinkedList<Map<String, Object>>();
        toMap(reports, messageType);

        AllocationStateReport allocationStateReport = new AllocationStateReport();
        for (Map<String, Object> report : reports) {
            allocationStateReport.addReport(report);
        }
        return allocationStateReport;
    }

    /**
     * @param reports to be filled by reports as maps
     * @param messageType to be used
     */
    private void toMap(List<Map<String, Object>> reports, MessageType messageType)
    {
        Map<String, Object> report = null;
        List<Map<String, Object>> childReports;
        if (isVisible(messageType)) {
            report = new MapReportSerializer(this);
            reports.add(report);
            childReports = new LinkedList<Map<String, Object>>();
        }
        else {
            childReports = reports;
        }
        if (this.childReports.size() > 0) {
            for (SchedulerReport childReport : this.childReports) {
                childReport.toMap(childReports, messageType);
            }
            if (report != null && childReports.size() > 0) {
                report.put(AllocationStateReport.CHILDREN, childReports);
            }
        }
    }

    /**
     * @param reports to be used
     * @param messageType to be used
     * @return given {@code reports} as {@link AllocationStateReport} for given {@code messageType}
     */
    public static AllocationStateReport getAllocationStateReport(List<SchedulerReport> reports, MessageType messageType)
    {
        List<Map<String, Object>> reportMaps = new LinkedList<Map<String, Object>>();
        for (SchedulerReport report : reports) {
            report.toMap(reportMaps, messageType);
        }

        AllocationStateReport allocationStateReport = new AllocationStateReport();
        for (Map<String, Object> report : reportMaps) {
            allocationStateReport.addReport(report);
        }
        return allocationStateReport;
    }
}
