package cz.cesnet.shongo.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Help class for print thread info
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ThreadHelper
{
    /**
     * Display information about a thread.
     */
    private static void printThreadInfo(PrintWriter out, Thread t, String indent)
    {
        if (t == null) {
            return;
        }
        out.println(indent + "Thread: " + t.getName() + "  Priority: " + t.getPriority()
                + (t.isDaemon() ? " Daemon" : "") + (t.isAlive() ? "" : " Not Alive"));
    }

    /**
     * Display info about a thread group and its threads and groups
     */
    private static void printGroupInfo(PrintWriter out, ThreadGroup g, String indent)
    {
        if (g == null) {
            return;
        }
        int num_threads = g.activeCount();
        int num_groups = g.activeGroupCount();
        Thread[] threads = new Thread[num_threads];
        ThreadGroup[] groups = new ThreadGroup[num_groups];

        g.enumerate(threads, false);
        g.enumerate(groups, false);

        out.println(indent + "Thread Group: " + g.getName() + "  Max Priority: " + g.getMaxPriority()
                + (g.isDaemon() ? " Daemon" : ""));

        for (int i = 0; i < num_threads; i++) {
            printThreadInfo(out, threads[i], indent + "    ");
        }
        for (int i = 0; i < num_groups; i++) {
            printGroupInfo(out, groups[i], indent + "    ");
        }
    }

    /**
     * Get root thread group
     *
     * @return root thread group
     */
    private static ThreadGroup getRootThreadGroup()
    {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        ThreadGroup parent = root.getParent();
        while (parent != null) {
            root = parent;
            parent = parent.getParent();
        }
        return root;
    }

    /**
     * Get thread group by name
     *
     * @param name
     * @param root
     * @return thread group or null
     */
    private static ThreadGroup getThreadGroupByName(String name, ThreadGroup root)
    {
        if (root == null) {
            root = getRootThreadGroup();
        }

        int num_groups = root.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[num_groups];
        root.enumerate(groups, false);

        for (int i = 0; i < num_groups; i++) {
            ThreadGroup threadGroup = groups[i];
            if (name.equals(threadGroup.getName())) {
                return threadGroup;
            }
            else {
                threadGroup = getThreadGroupByName(name, threadGroup);
                if (threadGroup != null) {
                    return threadGroup;
                }
            }
        }
        return null;
    }

    /**
     * Find the root thread group and list it recursively
     */
    private static void listAllThreads(PrintWriter out)
    {
        ThreadGroup root = getRootThreadGroup();
        printGroupInfo(out, root, "");
    }

    /**
     * Print all running threads
     */
    public static void printRunningThreads()
    {
        // Get the thread listing as a string using a StringWriter stream
        StringWriter stringWriter = new StringWriter();
        PrintWriter out = new PrintWriter(stringWriter);
        listAllThreads(out);
        out.close();
        String threadListing = stringWriter.toString();
        System.out.println(threadListing);
    }

    /**
     * @param name
     * @return list of threads
     */
    public static List<Thread> listThreadGroup(String name)
    {
        ThreadGroup threadGroup = getThreadGroupByName(name, null);
        if (threadGroup != null) {
            int count = threadGroup.activeCount();
            Thread[] threadArray = new Thread[count];
            threadGroup.enumerate(threadArray);
            return Arrays.asList(threadArray);
        }
        return List.of();
    }

    /**
     * Kill thread group by name
     *
     * @param name
     */
    public static void killThreadGroup(String name)
    {
        listThreadGroup(name).forEach(thread -> {
            if (thread == null) {
                return;
            }

            // TODO: Deprecated, replace with interrupt or jade.core.Runtime.instance().shutDown()
            thread.stop();
        });
    }
}
