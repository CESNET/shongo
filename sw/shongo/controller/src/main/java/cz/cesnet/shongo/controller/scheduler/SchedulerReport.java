package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
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
    public String getReportText()
    {
        StringBuilder stringBuilder = new StringBuilder();

        boolean hasChildReports = childReports.size() > 0;

        String text = getMessage();
        stringBuilder.append("-");
        switch (getType()) {
            case ERROR:
                stringBuilder.append("[ERROR] ");
                break;
            default:
                break;
        }
        if (hasChildReports) {
            text = text.replace("\n", String.format("\n  |%" + (stringBuilder.length() - 3) + "s", ""));
        }
        else {
            text = text.replace("\n", String.format("\n%" + stringBuilder.length() + "s", ""));
        }

        stringBuilder.append(text);

        if (hasChildReports) {
            int childReportsCount = childReports.size();
            for (int index = 0; index < childReportsCount; index++) {
                stringBuilder.append("\n  |");
                String childReportString = childReports.get(index).getReportText();
                childReportString = childReportString.replace("\n",
                        (index < (childReportsCount - 1) ? "\n  | " : "\n    "));
                stringBuilder.append("\n  +-");
                stringBuilder.append(childReportString);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString()
    {
        return getReportText();
    }
}
