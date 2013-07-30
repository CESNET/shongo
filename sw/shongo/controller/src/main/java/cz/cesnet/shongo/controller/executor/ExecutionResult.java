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
     * List of {@link cz.cesnet.shongo.controller.executor.Executable} which were updated.
     */
    private List<Executable> updatedExecutables = new ArrayList<Executable>();

    /**
     * List of {@link cz.cesnet.shongo.controller.executor.Executable} which were stopped.
     */
    private List<Executable> stoppedExecutables = new ArrayList<Executable>();

    /**
     * @param startedExecutable to be added to the {@link #startedExecutables}
     */
    public synchronized void addStartedExecutable(Executable startedExecutable)
    {
        startedExecutable.loadLazyProperties();
        startedExecutables.add(startedExecutable);
    }

    /**
     * @param updatedExecutable to be added to the {@link #updatedExecutables}
     */
    public synchronized void addUpdatedExecutable(Executable updatedExecutable)
    {
        updatedExecutable.loadLazyProperties();
        updatedExecutables.add(updatedExecutable);
    }

    /**
     * @param stoppedExecutable to be added to the {@link #stoppedExecutables}
     */
    public synchronized void addStoppedExecutable(Executable stoppedExecutable)
    {
        stoppedExecutable.loadLazyProperties();
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
     * @return {@link #updatedExecutables}
     */
    public synchronized List<Executable> getUpdatedExecutables()
    {
        return updatedExecutables;
    }

    /**
     * @return {@link #stoppedExecutables}
     */
    public synchronized List<Executable> getStoppedExecutables()
    {
        return stoppedExecutables;
    }
}
