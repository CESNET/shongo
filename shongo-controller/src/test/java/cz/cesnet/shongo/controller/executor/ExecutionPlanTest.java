package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.booking.executable.Executable;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link ExecutionPlan}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutionPlanTest
{
    @Test
    public void testCycle() throws Exception
    {
        final SimpleExecutable executable1 = new SimpleExecutable();
        final SimpleExecutable executable2 = new SimpleExecutable();
        final SimpleExecutable executable3 = new SimpleExecutable();
        executable1.addChildExecutable(executable2);
        executable2.addChildExecutable(executable3);
        executable3.addChildExecutable(executable1);

        try {
            ExecutionPlan executionPlan = new ExecutionPlan(null);
            executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable1));
            executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable2));
            executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable3));
            executionPlan.build();
            Assert.fail("Exception should be thrown (contains a cycle).");
        }
        catch (RuntimeException exception) {
        }

        try {
            ExecutionPlan executionPlan = new ExecutionPlan(null);
            executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable2));
            executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable3));
            executionPlan.build();

            checkExecutableAndRemove(executionPlan, executable3);
            checkExecutableAndRemove(executionPlan, executable2);
            Assert.assertTrue(executionPlan.isEmpty());
        }
        catch (RuntimeException exception) {
            exception.printStackTrace();
            Assert.fail("Exception should not be thrown (doesn't contain cycle).");
        }
    }

    @Test
    public void testOrder() throws Exception
    {
        final SimpleExecutable executable1 = new SimpleExecutable();
        final SimpleExecutable executable2 = new SimpleExecutable();
        final SimpleExecutable executable3 = new SimpleExecutable();
        executable1.addChildExecutable(executable2);
        executable2.addChildExecutable(executable3);

        ExecutionPlan executionPlan1 = new ExecutionPlan(null);
        executionPlan1.addExecutionAction(new ExecutionAction.StartExecutableAction(executable1));
        executionPlan1.addExecutionAction(new ExecutionAction.StartExecutableAction(executable2));
        executionPlan1.addExecutionAction(new ExecutionAction.StartExecutableAction(executable3));
        executionPlan1.build();

        checkExecutableAndRemove(executionPlan1, executable3);
        checkExecutableAndRemove(executionPlan1, executable2);
        checkExecutableAndRemove(executionPlan1, executable1);
        Assert.assertTrue(executionPlan1.isEmpty());

        ExecutionPlan executionPlan2 = new ExecutionPlan(null);
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable1));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable2));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable3));
        executionPlan2.build();

        checkExecutableAndRemove(executionPlan2, executable1);
        checkExecutableAndRemove(executionPlan2, executable2);
        checkExecutableAndRemove(executionPlan2, executable3);
        Assert.assertTrue(executionPlan2.isEmpty());
    }

    @Test
    public void testPriority() throws Exception
    {
        final SimpleExecutable executable1 = new SimpleExecutable();
        final SimpleExecutable executable2 = new SimpleExecutable();
        final SimpleExecutable executable3 = new SimpleExecutable();

        ExecutionPlan executionPlan1 = new ExecutionPlan(null);
        executionPlan1.addExecutionAction(new ExecutionAction.StartExecutableAction(executable1));
        executionPlan1.addExecutionAction(new ExecutionAction.UpdateExecutableAction(executable2));
        executionPlan1.addExecutionAction(new ExecutionAction.StopExecutableAction(executable3));
        executionPlan1.build();

        checkExecutableAndRemove(executionPlan1, executable3);
        checkExecutableAndRemove(executionPlan1, executable1);
        checkExecutableAndRemove(executionPlan1, executable2);
        Assert.assertTrue(executionPlan1.isEmpty());

        final SimpleExecutable executable4 = new SimpleExecutable();
        final SimpleExecutable executable5 = new SimpleExecutable();
        executable4.addChildExecutable(executable5);

        ExecutionPlan executionPlan2 = new ExecutionPlan(null);
        executionPlan2.addExecutionAction(new ExecutionAction.StartExecutableAction(executable1));
        executionPlan2.addExecutionAction(new ExecutionAction.UpdateExecutableAction(executable2));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable3));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable4));
        executionPlan2.addExecutionAction(new ExecutionAction.StartExecutableAction(executable5));
        executionPlan2.build();

        checkExecutableAndRemove(executionPlan2, executable3);
        checkExecutableAndRemove(executionPlan2, executable1, executable5);
        checkExecutableAndRemove(executionPlan2, executable4);
        checkExecutableAndRemove(executionPlan2, executable2);
        Assert.assertTrue(executionPlan2.isEmpty());
    }

    @Test
    public void testMigration() throws Exception
    {
        final SimpleExecutable executable1 = new SimpleExecutable();
        final SimpleExecutable executable2 = new SimpleExecutable();
        final SimpleExecutable executable3 = new SimpleExecutable();
        final SimpleExecutable executable4 = new SimpleExecutable();
        executable4.setSlot(new Interval("2014/2015"));
        executable4.setState(Executable.State.STARTED);
        final SimpleExecutable executable5 = new SimpleExecutable();
        executable5.setSlot(new Interval("2015/2016"));
        executable5.setState(Executable.State.NOT_STARTED);
        final Migration migration = new Migration(executable4, executable5)
        {
            @Override
            public boolean isReplacement()
            {
                return false;
            }
        };

        ExecutionPlan executionPlan2 = new ExecutionPlan(null);
        executionPlan2.addExecutionAction(new ExecutionAction.StartExecutableAction(executable1));
        executionPlan2.addExecutionAction(new ExecutionAction.UpdateExecutableAction(executable2));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable3));
        executionPlan2.addExecutionAction(new ExecutionAction.StopExecutableAction(executable4));
        executionPlan2.addExecutionAction(new ExecutionAction.StartExecutableAction(executable5));
        executionPlan2.addExecutionAction(new ExecutionAction.MigrationAction(migration));
        executionPlan2.build();

        checkExecutableAndRemove(executionPlan2, executable3);
        checkExecutableAndRemove(executionPlan2, executable1, executable5);
        checkMigrationAndRemove(executionPlan2, migration);
        checkExecutableAndRemove(executionPlan2, executable4);
        checkExecutableAndRemove(executionPlan2, executable2);
        Assert.assertTrue(executionPlan2.isEmpty());
    }

    private void checkExecutableAndRemove(ExecutionPlan executionPlan, Executable... executables)
    {
        Set<ExecutionAction.AbstractExecutableAction> executionActions =
                executionPlan.popExecutionActions(ExecutionAction.AbstractExecutableAction.class);
        Set<Executable> presentExecutables = new HashSet<Executable>();
        for (ExecutionAction.AbstractExecutableAction executionAction : executionActions) {
            presentExecutables.add(executionAction.getTarget());
        }
        Set<Executable> expectedExecutables = new HashSet<Executable>();
        Collections.addAll(expectedExecutables, executables);

        Assert.assertEquals(expectedExecutables, presentExecutables);

        for (ExecutionAction.AbstractExecutableAction executionAction : executionActions) {
            executionPlan.removeExecutionAction(executionAction);
        }
    }

    private void checkMigrationAndRemove(ExecutionPlan executionPlan, Migration... migrations)
    {
        Set<ExecutionAction.MigrationAction> executionActions =
                executionPlan.popExecutionActions(ExecutionAction.MigrationAction.class);
        Set<Migration> presentExecutables = new HashSet<Migration>();
        for (ExecutionAction.MigrationAction executionAction : executionActions) {
            presentExecutables.add(executionAction.getTarget());
        }
        Set<Migration> expectedExecutables = new HashSet<Migration>();
        Collections.addAll(expectedExecutables, migrations);

        Assert.assertEquals(expectedExecutables, presentExecutables);

        for (ExecutionAction.MigrationAction executionAction : executionActions) {
            executionPlan.removeExecutionAction(executionAction);
        }
    }

    public static class SimpleExecutable extends Executable
    {
        public SimpleExecutable()
        {
            generateTestingId();
        }
    }
}
