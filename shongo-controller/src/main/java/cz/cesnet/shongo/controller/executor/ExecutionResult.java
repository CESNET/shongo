package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.executable.Executable;

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
     * List of {@link Executable} which were started.
     */
    private List<Executable> startedExecutables = new ArrayList<Executable>();

    /**
     * List of {@link Executable} which were updated.
     */
    private List<Executable> updatedExecutables = new ArrayList<Executable>();

    /**
     * List of {@link Executable} which were stopped.
     */
    private List<Executable> stoppedExecutables = new ArrayList<Executable>();

    /**
     * List of {@link ExecutableService} which were activated.
     */
    private List<ExecutableService> activatedExecutableServices = new ArrayList<ExecutableService>();

    /**
     * List of {@link ExecutableService} which were deactivated.
     */
    private List<ExecutableService> deactivatedExecutableServices = new ArrayList<ExecutableService>();

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
     * @param activatedExecutableService to be added to the {@link #activatedExecutableServices}
     */
    public synchronized void addActivatedExecutableService(ExecutableService activatedExecutableService)
    {
        activatedExecutableServices.add(activatedExecutableService);
    }

    /**
     * @param deactivatedExecutableService to be added to the {@link #deactivatedExecutableServices}
     */
    public synchronized void addDeactivatedExecutableService(ExecutableService deactivatedExecutableService)
    {
        deactivatedExecutableServices.add(deactivatedExecutableService);
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

    /**
     * @return {@link #activatedExecutableServices}
     */
    public List<ExecutableService> getActivatedExecutableServices()
    {
        return activatedExecutableServices;
    }

    /**
     * @return {@link #deactivatedExecutableServices}
     */
    public List<ExecutableService> getDeactivatedExecutableServices()
    {
        return deactivatedExecutableServices;
    }
}
