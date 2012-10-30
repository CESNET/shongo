package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link AliasPatternGenerator}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasPatternGeneratorTest
{
    @Test
    public void testSinglePattern() throws Exception
    {
        AliasPatternGenerator generator;

        generator = new AliasPatternGenerator(Technology.H323, AliasType.E164, "950");
        assertEquals("950", generator.generate().getValue());
        assertEquals(null, generator.generate());

        generator = new AliasPatternGenerator(Technology.H323, AliasType.E164, "950[ddd]");
        generator.addAliasValue("950001");
        generator.addAliasValue("950003");
        generator.addAliasValue("950004");
        assertEquals("950002", generator.generate().getValue());
        assertEquals("950005", generator.generate().getValue());
        for (int index = 6; index <= 999; index++) {
            assertEquals(String.format("950%03d", index), generator.generate().getValue());
        }
        assertEquals(null, generator.generate());

        generator = new AliasPatternGenerator(Technology.H323, AliasType.E164, "950[d]00[d]");
        generator.addAliasValue("9500001");
        generator.addAliasValue("9500003");
        generator.addAliasValue("9500004");
        assertEquals("9500002", generator.generate().getValue());
        assertEquals("9500005", generator.generate().getValue());
        for (int index = 6; index <= 99; index++) {
            assertEquals(String.format("950%01d00%01d", index / 10, index % 10), generator.generate().getValue());
        }
        assertEquals(null, generator.generate());
    }

    @Test
    public void testMultiplePatterns() throws Exception
    {
        AliasPatternGenerator generator;

        generator = new AliasPatternGenerator(Technology.H323, AliasType.E164);
        generator.addPattern("9501");
        generator.addPattern("9502");
        assertEquals("9501", generator.generate().getValue());
        assertEquals("9502", generator.generate().getValue());
        assertEquals(null, generator.generate());

        generator = new AliasPatternGenerator(Technology.H323, AliasType.E164);
        generator.addPattern("9501[d]");
        generator.addPattern("9502[d]");
        for (int index = 1; index <= 9; index++) {
            assertEquals(String.format("9501%d", index), generator.generate().getValue());
        }
        for (int index = 1; index <= 9; index++) {
            assertEquals(String.format("9502%d", index), generator.generate().getValue());
        }
        assertEquals(null, generator.generate());
    }
}
