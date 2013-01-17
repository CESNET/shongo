package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.resource.value.Pattern;
import cz.cesnet.shongo.controller.resource.value.PatternValueProvider;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PatternValueProviderTest
{
    @Test
    public void testSinglePattern() throws Exception
    {
        PatternValueProvider generator;
        Set<String> generatedValues;

        generator = new PatternValueProvider(null, "950");
        generatedValues = new HashSet<String>();
        Assert.assertEquals("950", generator.generateAddedValue(generatedValues));
        Assert.assertEquals(null, generator.generateAddedValue(generatedValues));

        generator = new PatternValueProvider(null, "950{digit:3}");
        generatedValues = new HashSet<String>();
        generatedValues.add("950001");
        generatedValues.add("950003");
        generatedValues.add("950004");
        Assert.assertEquals("950002", generator.generateAddedValue(generatedValues));
        Assert.assertEquals("950005", generator.generateAddedValue(generatedValues));
        for (int index = 6; index <= 999; index++) {
            Assert.assertEquals(String.format("950%03d", index), generator.generateAddedValue(generatedValues));
        }
        Assert.assertEquals(null, generator.generateAddedValue(generatedValues));

        generator = new PatternValueProvider(null, "950{digit:1}00{digit:1}");
        generatedValues = new HashSet<String>();
        generatedValues.add("9500001");
        generatedValues.add("9500003");
        generatedValues.add("9500004");
        Assert.assertEquals("9500002", generator.generateAddedValue(generatedValues));
        Assert.assertEquals("9500005", generator.generateAddedValue(generatedValues));
        for (int index = 6; index <= 99; index++) {
            Assert.assertEquals(String.format("950%01d00%01d", index / 10, index % 10),
                    generator.generateAddedValue(generatedValues));
        }
        Assert.assertEquals(null, generator.generateAddedValue(generatedValues));
    }

    @Test
    public void testMultiplePatterns() throws Exception
    {
        PatternValueProvider generator;
        Set<String> generatedValues;

        generator = new PatternValueProvider();
        generatedValues = new HashSet<String>();
        generator.addPattern("9501");
        generator.addPattern("9502");
        Assert.assertEquals("9501", generator.generateAddedValue(generatedValues));
        Assert.assertEquals("9502", generator.generateAddedValue(generatedValues));
        Assert.assertEquals(null, generator.generateAddedValue(generatedValues));

        generator = new PatternValueProvider();
        generatedValues = new HashSet<String>();
        generator.addPattern("9501{digit:1}");
        generator.addPattern("9502{digit:1}");
        for (int index = 1; index <= 9; index++) {
            Assert.assertEquals(String.format("9501%d", index), generator.generateAddedValue(generatedValues));
        }
        for (int index = 1; index <= 9; index++) {
            Assert.assertEquals(String.format("9502%d", index), generator.generateAddedValue(generatedValues));
        }
        Assert.assertEquals(null, generator.generateAddedValue(generatedValues));
    }

    @Test
    public void testHashPatternLength() throws Exception
    {
        PatternValueProvider generator = new PatternValueProvider(null, "{hash:6}");
        Assert.assertEquals(6, generator.generateValue(new HashSet<String>()).length());
        generator = new PatternValueProvider(null, "{hash:10}");
        Assert.assertEquals(10, generator.generateValue(new HashSet<String>()).length());
        generator = new PatternValueProvider(null, "{hash}");
        Assert.assertEquals(Pattern.HashPatternComponent.DEFAULT_LENGTH,
                generator.generateValue(new HashSet<String>()).length());
    }

    @Test
    public void testRequestedValues() throws Exception
    {
        PatternValueProvider generator = new PatternValueProvider();
        generator.addPattern("test1 {hash:4}");
        generator.addPattern("test2 {hash:5}");
        Assert.assertEquals("test1 xxxx", generator.generateValue(new HashSet<String>(), "test1 xxxx"));
        Assert.assertEquals("test2 yyyyy", generator.generateValue(new HashSet<String>(), "test2 yyyyy"));
        Assert.assertNull(generator.generateValue(new HashSet<String>(), "test1 xxxxx"));
        Assert.assertNull(generator.generateValue(new HashSet<String>(), "test2 xxxx"));
        Assert.assertNull(generator.generateValue(new HashSet<String>(), "test yyyyy"));
    }
}
