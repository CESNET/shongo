package cz.cesnet.shongo.controller.executor.report;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.fault.jade.CommandFailure;
import org.joda.time.DateTime;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class UsedRoomNotExistReport extends ExecutableReport
{
    /**
     * Constructor.
     */
    public UsedRoomNotExistReport()
    {
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return "Cannot modify room, because it has not been created.";
    }
}
