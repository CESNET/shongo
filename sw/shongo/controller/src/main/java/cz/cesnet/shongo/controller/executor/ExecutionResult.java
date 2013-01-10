package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.Executor;

import java.util.ArrayList;
import java.util.List;

/**
* Result of the {@link Executor#execute(org.joda.time.DateTime)}.
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class ExecutionResult
{
    /**
     * List of {@link cz.cesnet.shongo.controller.executor.Executable} which were started.
     */
    private List<Executable> startedExecutables = new ArrayList<Executable>();

    /**
     * List of {@link cz.cesnet.shongo.controller.executor.Executable} which were stopped.
     */
    private List<Executable> stoppedExecutables = new ArrayList<Executable>();

    /**
     * @param startedExecutable to be added to the {@link #startedExecutables}
     */
    public synchronized void addStartedExecutable(Executable startedExecutable)
    {
        startedExecutables.add(startedExecutable);
    }

    /**
     * @param stoppedExecutable to be added to the {@link #stoppedExecutables}
     */
    public synchronized void addStoppedExecutable(Executable stoppedExecutable)
    {
        stoppedExecutables.add(stoppedExecutable);
    }

    /**
     * @return {@link #startedExecutables}
     */
    public synchronized List<Executable> getStartedExecutables()
    {
        return startedExecutables;
    }

    /**
     * @return {@link #stoppedExecutables}
     */
    public synchronized List<Executable> getStoppedExecutables()
    {
        return stoppedExecutables;
    }
}
