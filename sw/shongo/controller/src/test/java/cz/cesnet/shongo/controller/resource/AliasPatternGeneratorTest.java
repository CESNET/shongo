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

        generator = new AliasPatternGenerator("950");
        assertEquals("950", generator.generateValue());
        assertEquals(null, generator.generateValue());

        generator = new AliasPatternGenerator("950{digit:3}");
        generator.addAliasValue("950001");
        generator.addAliasValue("950003");
        generator.addAliasValue("950004");
        assertEquals("950002", generator.generateValue());
        assertEquals("950005", generator.generateValue());
        for (int index = 6; index <= 999; index++) {
            assertEquals(String.format("950%03d", index), generator.generateValue());
        }
        assertEquals(null, generator.generateValue());

        generator = new AliasPatternGenerator("950{digit:1}00{digit:1}");
        generator.addAliasValue("9500001");
        generator.addAliasValue("9500003");
        generator.addAliasValue("9500004");
        assertEquals("9500002", generator.generateValue());
        assertEquals("9500005", generator.generateValue());
        for (int index = 6; index <= 99; index++) {
            assertEquals(String.format("950%01d00%01d", index / 10, index % 10), generator.generateValue());
        }
        assertEquals(null, generator.generateValue());
    }

    @Test
    public void testMultiplePatterns() throws Exception
    {
        AliasPatternGenerator generator;

        generator = new AliasPatternGenerator();
        generator.addPattern("9501");
        generator.addPattern("9502");
        assertEquals("9501", generator.generateValue());
        assertEquals("9502", generator.generateValue());
        assertEquals(null, generator.generateValue());

        generator = new AliasPatternGenerator();
        generator.addPattern("9501{digit:1}");
        generator.addPattern("9502{digit:1}");
        for (int index = 1; index <= 9; index++) {
            assertEquals(String.format("9501%d", index), generator.generateValue());
        }
        for (int index = 1; index <= 9; index++) {
            assertEquals(String.format("9502%d", index), generator.generateValue());
        }
        assertEquals(null, generator.generateValue());
    }

    @Test
    public void testStringPattern() throws Exception
    {
        AliasPatternGenerator generator = new AliasPatternGenerator("{string}");
        assertEquals(AliasPatternGenerator.Pattern.STRING_PATTERN_LENGTH, generator.generateValue().length());
    }
}
