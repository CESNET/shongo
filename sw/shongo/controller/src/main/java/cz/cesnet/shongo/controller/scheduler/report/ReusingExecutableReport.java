package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.executor.Executable;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ReusingExecutableReport extends AbstractExecutableReport
{
    /**
     * Constructor.
     */
    private ReusingExecutableReport()
    {
    }

    /**
     * Constructor.
     */
    public ReusingExecutableReport(Executable executable)
    {
        super(executable);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Reusing %s", getExecutableDescription());
    }
}
