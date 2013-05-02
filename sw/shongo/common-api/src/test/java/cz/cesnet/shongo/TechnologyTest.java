package cz.cesnet.shongo;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Technology}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TechnologyTest
{
    /**
     * @param array
     * @return set of technologies from array of technologies
     */
    private static Set<Technology> buildSet(Technology[] array)
    {
        Set<Technology> set = new HashSet<Technology>();
        for (Technology technology : array) {
            set.add(technology);
        }
        return set;
    }

    /**
     * @param arrays
     * @return set of sets from array of arrays of {@link Technology}
     */
    private static Set<Set<Technology>> buildSet(Technology[][] arrays)
    {
        Set<Set<Technology>> sets = new HashSet<Set<Technology>>();
        for (Technology[] array : arrays) {
            sets.add(buildSet(array));
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
    public void testInterconnect() throws Exception
    {
        List<Set<Technology>> technologySets = new ArrayList<Set<Technology>>();

        technologySets.clear();
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY3, TECHNOLOGY4}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY5, TECHNOLOGY6}));
        Assert.assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY4, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY4, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY3, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4, TECHNOLOGY6},
        }), Technology.interconnect(technologySets));

        technologySets.clear();
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY1, TECHNOLOGY6}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY5, TECHNOLOGY6}));
        Assert.assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY6},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY6}
        }), Technology.interconnect(technologySets));

        technologySets.clear();
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY1, TECHNOLOGY2}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY2, TECHNOLOGY3}));
        technologySets.add(buildSet(new Technology[]{TECHNOLOGY4, TECHNOLOGY5}));
        Assert.assertEquals(buildSet(new Technology[][]{
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY4},
                new Technology[]{TECHNOLOGY1, TECHNOLOGY3, TECHNOLOGY5},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY4},
                new Technology[]{TECHNOLOGY2, TECHNOLOGY5},
        }), Technology.interconnect(technologySets));
    }
}
