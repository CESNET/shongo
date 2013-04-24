package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class SchedulerReport extends Report
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

    /**
     * Parent {@link Report} to which it belongs.
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
     * @return formatted text and help of the {@link Report}
     */
    @Transient
    public String getMessageRecursive(MessageType messageType)
    {
        boolean isVisible = !messageType.equals(MessageType.USER) || isVisible(VISIBLE_TO_USER);

        // Build message and prefix for this report
        StringBuilder messagePrefix = new StringBuilder();
        String message = null;
        if (isVisible) {
            message = getMessage(messageType);
            messagePrefix.append("-");
            switch (getType()) {
                case ERROR:
                    messagePrefix.append("[ERROR] ");
                    break;
                default:
                    break;
            }
        }

        // Build messages for child reports
        StringBuilder childMessages = new StringBuilder();
        int childReportsCount = childReports.size();
        for (int index = 0; index < childReportsCount; index++) {
            String childReportString = childReports.get(index).getMessageRecursive(messageType);
            if (childReportString != null) {
                if (message != null) {
                    childMessages.append("\n  |");
                    childMessages.append("\n  +-");
                    childReportString = childReportString.replace("\n",
                            (index < (childReportsCount - 1) ? "\n  | " : "\n    "));
                }
                else {
                    if (childMessages.length() > 0) {
                        childMessages.append("\n\n");
                    }
                }
                childMessages.append(childReportString);
            }
        }

        StringBuilder messageBuilder = new StringBuilder();
        if (message != null) {
            if (childMessages.length() > 0) {
                message = message.replace("\n", String.format("\n  |%" + (messagePrefix.length() - 3) + "s", ""));
            }
            else {
                message = message.replace("\n", String.format("\n%" + messagePrefix.length() + "s", ""));
            }
            messageBuilder.append(messagePrefix);
            messageBuilder.append(message);
        }
        if (childMessages.length() > 0) {
            messageBuilder.append(childMessages);
        }
        return (messageBuilder.length() > 0 ? messageBuilder.toString() : null);
    }

    @Override
    public String toString()
    {
        return getMessageRecursive(MessageType.DOMAIN_ADMIN);
    }
}
