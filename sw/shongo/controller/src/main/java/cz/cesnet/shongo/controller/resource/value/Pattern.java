package cz.cesnet.shongo.controller.resource.value;

import org.apache.commons.lang.RandomStringUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;

/**
 * Value pattern for {@link PatternValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Pattern extends ArrayList<Pattern.PatternComponent>
{
    /**
     * Length of automatic generated strings by "{string}" pattern.
     */
    public static final int STRING_PATTERN_LENGTH = 8;

    /**
     * Regex for {@link Pattern.NumberPatternComponent}.
     */
    private static final java.util.regex.Pattern NUMBER_PATTERN = java.util.regex.Pattern.compile("digit:(\\d+)");

    /**
     * Regex for {@link Pattern.StringPatternComponent}
     */
    private static final java.util.regex.Pattern STRING_PATTERN = java.util.regex.Pattern.compile("string");

    /**
     * Single value pattern.
     */
    private boolean singleValuePattern = false;

    /**
     * Generated count.
     */
    private int generatedCount = 0;

    /**
     * Parse pattern from string.
     *
     * @param pattern string to be parsed
     */
    public void parse(String pattern)
    {
        int start = -1;
        int end = -1;
        while ((start = pattern.indexOf('{')) != -1 && (end = pattern.indexOf('}')) != -1) {
            if (start > 0) {
                add(new Pattern.ConstantPatternComponent(pattern.substring(0, start)));
            }
            String component = pattern.substring(start + 1, end);
            Matcher numberMatcher = NUMBER_PATTERN.matcher(component);
            if (numberMatcher.matches()) {
                add(new Pattern.NumberPatternComponent(Integer.valueOf(numberMatcher.group(1))));
            }
            else if (STRING_PATTERN.matcher(component).matches()) {
                add(new Pattern.StringPatternComponent());
            }
            else {
                throw new IllegalArgumentException("Component '{" + component + "}' is in wrong format.");
            }
            pattern = pattern.substring(end + 1);
        }
        if (pattern.length() > 0) {
            add(new Pattern.ConstantPatternComponent(pattern));
        }
        singleValuePattern = size() == 1 && get(0) instanceof Pattern.ConstantPatternComponent;
    }

    /**
     * Reset all pattern components
     */
    public void reset()
    {
        generatedCount = 0;
        for (Pattern.PatternComponent patternComponent : this) {
            if (patternComponent instanceof Pattern.GeneratedPatternComponent) {
                Pattern.GeneratedPatternComponent generatedPatternComponent =
                        (Pattern.GeneratedPatternComponent) patternComponent;
                generatedPatternComponent.reset();
            }
        }
    }

    /**
     * @return new generated value from the pattern
     */
    public String generate()
    {
        StringBuilder builder = new StringBuilder();
        boolean performNextComponent = true;
        for (int index = size() - 1; index >= 0; index--) {
            PatternComponent patternComponent = get(index);
            if (patternComponent instanceof Pattern.GeneratedPatternComponent) {
                Pattern.GeneratedPatternComponent generatedPatternComponent =
                        (Pattern.GeneratedPatternComponent) patternComponent;
                if (performNextComponent) {
                    generatedPatternComponent.nextComponent();
                }
                if (generatedPatternComponent.available()) {
                    performNextComponent = false;
                }
                else {
                    performNextComponent = true;
                    generatedPatternComponent.reset();
                }
            }
            builder.insert(0, patternComponent.getComponent());
        }

        if (performNextComponent && (!singleValuePattern || generatedCount > 0)) {
            return null;
        }
        generatedCount++;
        return builder.toString();
    }

    /**
     * {@link Pattern} component.
     */
    public interface PatternComponent
    {
        /**
         * @return new value for pattern component if possible, null otherwise
         */
        public String getComponent();
    }

    /**
     * {@link PatternComponent} which returns always same string.
     */
    static class ConstantPatternComponent implements PatternComponent
    {
        /**
         * Same string which is returned
         */
        private String component;

        /**
         * Constructor.
         *
         * @param component sets the {@link #component}
         */
        public ConstantPatternComponent(String component)
        {
            this.component = component;
        }

        @Override
        public String getComponent()
        {
            return component;
        }
    }

    /**
     * {@link PatternComponent} which returns generated component.
     */
    static interface GeneratedPatternComponent extends PatternComponent
    {
        /**
         * Reset generating.
         */
        public void reset();

        /**
         * Generate next component.
         */
        public void nextComponent();

        /**
         * @return true if next generated value is available, false otherwise
         */
        public boolean available();
    }

    /**
     * {@link PatternComponent} which returns increasing numbers of given length.
     */
    static class NumberPatternComponent implements GeneratedPatternComponent
    {
        /**
         * Format for numbers.
         */
        private String format;

        /**
         * Current number.
         */
        private int currentValue;

        /**
         * Maximum number which is possible for given length.
         */
        private int maxValue;

        /**
         * Constructor.
         *
         * @param length maximum returned number length
         */
        public NumberPatternComponent(int length)
        {
            if (length < 1 || length > 10) {
                throw new IllegalArgumentException("Length of number component should be in range from 1 to 10.");
            }
            this.format = "%0" + Integer.valueOf(length).toString() + "d";
            this.maxValue = (int) Math.pow(10, length) - 1;
            reset();
        }

        @Override
        public void nextComponent()
        {
            currentValue++;
        }

        @Override
        public String getComponent()
        {
            if (currentValue > maxValue) {
                return null;
            }
            return String.format(format, currentValue);
        }

        @Override
        public void reset()
        {
            currentValue = 0;
        }

        @Override
        public boolean available()
        {
            return currentValue <= maxValue;
        }
    }

    /**
     * {@link PatternComponent} which returns increasing numbers of given length.
     */
    static class StringPatternComponent implements GeneratedPatternComponent
    {
        /**
         * Pattern for matching correct values.
         */
        static final java.util.regex.Pattern VALUE_PATTERN =
                java.util.regex.Pattern.compile("^\\p{Alpha}[\\p{Alnum}_-]*$");

        /**
         * Current number.
         */
        private String currentValue;

        /**
         * Constructor.
         */
        public StringPatternComponent()
        {
            reset();
        }

        @Override
        public void nextComponent()
        {
            currentValue = RandomStringUtils.randomAlphabetic(1).toLowerCase();
            currentValue += RandomStringUtils.randomAlphanumeric(Pattern.STRING_PATTERN_LENGTH - 1).toLowerCase();
        }

        @Override
        public String getComponent()
        {
            return currentValue;
        }

        @Override
        public void reset()
        {
        }

        @Override
        public boolean available()
        {
            return true;
        }
    }
}
