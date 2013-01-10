package cz.cesnet.shongo.controller.executor;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

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
        final SimpleExecutable executable4 = new SimpleExecutable();
        final SimpleExecutable executable5 = new SimpleExecutable();

        executable1.addChildExecutable(executable5);
        executable2.addChildExecutable(executable1);
        executable3.addChildExecutable(executable2);
        executable4.addChildExecutable(executable3);
        executable5.addChildExecutable(executable4);

        try {
            new ExecutionPlan(new ArrayList<Executable>()
            {{
                    add(executable1);
                    add(executable2);
                    add(executable3);
                    add(executable4);
                    add(executable5);
                }});
            Assert.fail("Exception should be thrown (contains cycle).");
        } catch (IllegalStateException exception) {
        }

        try {
            ExecutionPlan executionPlan = new ExecutionPlan(new ArrayList<Executable>()
            {{
                    add(executable2);
                    add(executable3);
                    add(executable4);
                    add(executable5);
                }});
            Collection<Executable> executables;
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable2, executables.iterator().next());

            executionPlan.removeExecutable(executable2);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable3, executables.iterator().next());

            executionPlan.removeExecutable(executable3);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable4, executables.iterator().next());

            executionPlan.removeExecutable(executable4);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable5, executables.iterator().next());
        } catch (IllegalStateException exception) {
            Assert.fail("Exception should not be thrown (doesn't contain cycle).");
        }

        try {
            ExecutionPlan executionPlan = new ReverseExecutionPlan(new ArrayList<Executable>()
            {{
                    add(executable2);
                    add(executable3);
                    add(executable4);
                    add(executable5);
                }});
            Collection<Executable> executables;
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable5, executables.iterator().next());

            executionPlan.removeExecutable(executable5);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable4, executables.iterator().next());

            executionPlan.removeExecutable(executable4);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable3, executables.iterator().next());

            executionPlan.removeExecutable(executable3);
            executables = executionPlan.popExecutables();
            Assert.assertEquals(1, executables.size());
            Assert.assertEquals(executable2, executables.iterator().next());
        } catch (IllegalStateException exception) {
            Assert.fail("Exception should not be thrown (doesn't contain cycle).");
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
