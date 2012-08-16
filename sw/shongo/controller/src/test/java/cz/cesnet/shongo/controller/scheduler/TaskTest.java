package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link Task}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TaskTest
{
    /**
     * @param arrays
     * @return set of sets from array of arrays of {@link Technology}
     */
    private Set<Set<Technology>> buildSet(Technology[][] arrays)
    {
        Set<Set<Technology>> sets = new HashSet<Set<Technology>>();
        for (Technology[] array : arrays) {
            Set<Technology> set = new HashSet<Technology>();
            for (Technology technology : array) {
                set.add(technology);
            }
            sets.add(set);
        }
        return sets;
    }

    private static final Technology TECHNOLOGY1 = Technology.H323;
    private static final Technology TECHNOLOGY2 = Technology.SIP;
    private static final Technology TECHNOLOGY3 = Technology.ADOBE_CONNECT;
    private static final Technology TECHNOLOGY4 = Technology.SKYPE;
    private static final Technology TECHNOLOGY5 = Technology.BIG_BLUE_BUTTON;
    private static final Technology TECHNOLOGY6 = Technology.OPEN_MEETINGS;

    @Test
    public void testGetTechnologiesForSingleVirtualRoom() throws Exception
    {
        Task task1 = new Task();
        task1.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        task1.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY3, TECHNOLOGY4}));
        task1.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY5, TECHNOLOGY6}));
        task1.mergeInterconnectableGroups();
        assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY4, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY4, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY3, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4, TECHNOLOGY6},
        }), task1.getInterconnectingTechnologies());

        Task task2 = new Task();
        task2.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        task2.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY1, TECHNOLOGY6}));
        task2.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY5, TECHNOLOGY6}));
        task2.getInterconnectingTechnologies();
        assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY6}
        }), task2.getInterconnectingTechnologies());

        Task task3 = new Task();
        task3.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        task3.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY2, TECHNOLOGY3}));
        task3.addInterconnectableGroup(new InterconnectableGroup(new Technology[]{TECHNOLOGY4, TECHNOLOGY5}));
        task3.getInterconnectingTechnologies();
        assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY4},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY5},
        }), task3.getInterconnectingTechnologies());
    }
}
