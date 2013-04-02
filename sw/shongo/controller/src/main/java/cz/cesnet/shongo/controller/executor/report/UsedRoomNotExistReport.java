package cz.cesnet.shongo.controller.executor.report;

import javax.persistence.Entity;
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
