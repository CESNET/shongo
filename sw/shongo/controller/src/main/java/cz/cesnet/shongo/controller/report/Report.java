package cz.cesnet.shongo.controller.report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a report (e.g., text message) describing some event concerning {@link ReportablePersistentObject}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@DiscriminatorColumn(length = 50)
public abstract class Report
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

    /**
     * Parent {@link Report} to which it belongs.
     */
    private Report parentReport;

    /**
     * {@link State} of the {@link Report}.
     */
    private State state;

    /**
     * List of child resources (e.g., physical room can contain some videoconferencing equipment).
     */
    private List<Report> childReports = new ArrayList<Report>();

    /**
     * Constructor.
     */
    public Report()
    {
        // Set default state to OK
        State state = getState();
        if (state == null) {
            state = State.OK;
        }
        setState(state);
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
    public Report getParentReport()
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
    public void setParentReport(Report parentReport)
    {
        // Manage bidirectional association
        if (parentReport != this.parentReport) {
            if (this.parentReport != null) {
                Report oldParentMessage = this.parentReport;
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
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * @return {@link #childReports}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentReport")
    @Access(AccessType.FIELD)
    public List<Report> getChildReports()
    {
        return childReports;
    }

    /**
     * @param childReports sets the {@link #childReports}
     */
    public void setChildReports(List<Report> childReports)
    {
        this.childReports = childReports;
    }

    /**
     * @param report to be added to the {@link #childReports}
     */
    public void addChildReport(Report report)
    {
        // Manage bidirectional association
        if (childReports.contains(report) == false) {
            childReports.add(report);
            report.setParentReport(this);
        }
    }

    /**
     * @param report    to be searched in the {@link #childReports} and removed
     * @param replaceBy to be added to the {@link #childReports} at the index of {@code report}
     */
    public void replaceChildReport(Report report, Report replaceBy)
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
    public void addChildReports(Collection<Report> reports)
    {
        for (Report report : reports) {
            addChildReport(report);
        }
    }

    /**
     * @param message to be removed from the {@link #childReports}
     */
    public void removeChildReport(Report message)
    {
        // Manage bidirectional association
        if (childReports.contains(message)) {
            childReports.remove(message);
            message.setParentReport(null);
        }
    }

    /**
     * @return text of the {@link Report}
     */
    @Transient
    public abstract String getText();

    /**
     * @return help of the {@link Report}
     */
    @Transient
    public String getHelp()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getReport();
    }

    /**
     * @return formatted text and help of the {@link Report}
     */
    @Transient
    public String getReport()
    {
        StringBuilder stringBuilder = new StringBuilder();

        boolean hasChildReports = childReports.size() > 0;

        String text = getText();
        stringBuilder.append("-");
        if (state != null) {
            switch (getState()) {
                case OK:
                    stringBuilder.append("[OK] ");
                    break;
                case ERROR:
                    stringBuilder.append("[ERROR] ");
                    break;
                default:
                    break;
            }
        }
        if (hasChildReports) {
            text = text.replace("\n", String.format("\n  |%" + (stringBuilder.length() - 3) + "s", ""));
        }
        else {
            text = text.replace("\n", String.format("\n%" + stringBuilder.length() + "s", ""));
        }

        stringBuilder.append(text);

        String help = getHelp();
        if (help != null) {
            help = help.replace("\n", "\n ");
            stringBuilder.append("\n ");
            stringBuilder.append(help);
        }

        if (hasChildReports) {
            int childReportsCount = childReports.size();
            for (int index = 0; index < childReportsCount; index++) {
                stringBuilder.append("\n  |");
                String childReportString = childReports.get(index).getReport();
                childReportString = childReportString.replace("\n",
                        (index < (childReportsCount - 1) ? "\n  | " : "\n    "));
                stringBuilder.append("\n  +-");
                stringBuilder.append(childReportString);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * @return {@link ReportException} with this {@link Report} as description
     */
    public ReportException exception()
    {
        if (getState() != State.ERROR) {
            throw new IllegalStateException("Report exception can be returned only for errors.");
        }
        return new ReportException(this);
    }

    /**
     * Type of {@link Report}.
     */
    public static enum State
    {
        /**
         * Represents no state for s{@link Report}.
         */
        NONE,

        /**
         * Represents an warning {@link Report}.
         */
        OK,

        /**
         * Represents an error {@link Report}.
         */
        ERROR
    }
}
