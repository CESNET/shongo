package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public abstract class AbstractExecutableReport extends Report
{
    /**
     * @see Executable
     */
    private Executable executable;

    /**
     * Constructor.
     */
    protected AbstractExecutableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param executable sets the {@link #executable}
     */
    public AbstractExecutableReport(Executable executable)
    {
        setExecutable(executable);
    }

    /**
     * @return {@link #executable}
     */
    @OneToOne
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }

    /**
     * @return string description of executable
     */
    @Transient
    public String getExecutableDescription()
    {
        return String.format("executable '%s'", IdentifierFormat.formatGlobalId(executable));
    }
}
