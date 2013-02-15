package cz.cesnet.shongo.controller.report;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a {@link PersistentObject} which can contain one or more {@link Report}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class ReportablePersistentObject extends PersistentObject
{
    /**
     * List of report for this object.
     */
    private List<Report> reports = new ArrayList<Report>();

    /**
     * @return {@link #reports}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<Report> getReports()
    {
        return Collections.unmodifiableList(reports);
    }

    /**
     * @param reports sets the {@link #reports}
     */
    public void setReports(List<Report> reports)
    {
        this.reports.clear();
        for (Report report : reports) {
            this.reports.add(report);
        }
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    public void addReport(Report report)
    {
        reports.add(report);
    }

    /**
     * @param report to be removed from the {@link #reports}
     */
    public void removeReport(Report report)
    {
        reports.remove(report);
    }

    /**
     * Remove all {@link Report}s from the {@link #reports}.
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * @return string for report header
     */
    @Transient
    public String getReportHeader()
    {
        return null;
    }

    /**
     * @return report string containing report header and all {@link #reports}
     */
    @Transient
    public String getReportText()
    {
        StringBuilder stringBuilder = new StringBuilder();
        String reportHeader = getReportHeader();
        if (reportHeader != null) {
            stringBuilder.append(reportHeader);
        }
        for (Report report : reports) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }
            stringBuilder.append(report.getReport());
        }
        return (stringBuilder.length() > 0 ? stringBuilder.toString() : null);
    }
}
