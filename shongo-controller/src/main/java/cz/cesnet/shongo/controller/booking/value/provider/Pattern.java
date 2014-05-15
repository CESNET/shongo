package cz.cesnet.shongo.controller.booking.value.provider;

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
     * Regex for {@link Pattern.DigitPatternComponent}.
     */
    private static final java.util.regex.Pattern DIGIT_PATTERN = java.util.regex.Pattern.compile("digit:(\\d+)");

    /**
     * Regex for {@link Pattern.DigitPatternComponent}.
     */
    private static final java.util.regex.Pattern NUMBER_RANGE_PATTERN =
            java.util.regex.Pattern.compile("number:(\\d+):(\\d+)");

    /**
     * Regex for {@link Pattern.HashPatternComponent}
     */
    private static final java.util.regex.Pattern HASH_PATTERN = java.util.regex.Pattern.compile("hash(:(\\d+))?");

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
            Matcher numberMatcher = DIGIT_PATTERN.matcher(component);
            Matcher numberRangeMatcher = NUMBER_RANGE_PATTERN.matcher(component);
            Matcher hashMatcher = HASH_PATTERN.matcher(component);
            if (numberMatcher.matches()) {
                add(new DigitPatternComponent(Integer.valueOf(numberMatcher.group(1))));
            }
            else if (numberRangeMatcher.matches()) {
                add(new NumberRangePatternComponent(numberRangeMatcher.group(1), numberRangeMatcher.group(2)));
            }
            else if (hashMatcher.matches()) {
                add(new HashPatternComponent((hashMatcher.group(2) != null
                                                      ? Integer.valueOf(hashMatcher.group(2))
                                                      : HashPatternComponent.DEFAULT_LENGTH)));
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
            builder.insert(0, patternComponent.getConstant());
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
        public String getConstant();

        /**
         * @return regex pattern for allowed values
         */
        public String getRegexPattern();

        /**
         * @param value which has already passed by the pattern from the {@link #getRegexPattern()}
         * @return true if given {@code value} is valid, false otherwise
         */
        boolean isValueValid(String value);
    }

    /**
     * {@link PatternComponent} which returns always same string.
     */
    public static class ConstantPatternComponent implements PatternComponent
    {
        /**
         * Same string which is returned
         */
        private String constant;

        /**
         * Constructor.
         *
         * @param constant sets the {@link #constant}
         */
        public ConstantPatternComponent(String constant)
        {
            this.constant = constant;
        }

        @Override
        public String getConstant()
        {
            return constant;
        }

        @Override
        public String getRegexPattern()
        {
            return java.util.regex.Pattern.quote(constant);
        }

        @Override
        public boolean isValueValid(String value)
        {
            return true;
        }
    }

    /**
     * {@link PatternComponent} which returns generated component.
     */
    public static interface GeneratedPatternComponent extends PatternComponent
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
    public static class DigitPatternComponent implements GeneratedPatternComponent
    {
        /**
         * Maximum length.
         */
        private int length;

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
        public DigitPatternComponent(int length)
        {
            if (length < 1 || length > 10) {
                throw new IllegalArgumentException("Length of number component should be in range from 1 to 10.");
            }
            this.length = length;
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
        public String getConstant()
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

        @Override
        public String getRegexPattern()
        {
            StringBuilder regexPatternBuilder = new StringBuilder();
            regexPatternBuilder.append("\\d{");
            regexPatternBuilder.append(length);
            regexPatternBuilder.append("}");
            return regexPatternBuilder.toString();
        }

        @Override
        public boolean isValueValid(String value)
        {
            return true;
        }
    }

    /**
     * {@link PatternComponent} which returns increasing numbers of given length.
     */
    public static class NumberRangePatternComponent implements GeneratedPatternComponent
    {
        /**
         * Min value.
         */
        private int minValue;

        /**
         * Max value.
         */
        private int maxValue;

        /**
         * Format for numbers.
         */
        private String format;

        /**
         * Regex pattern.
         */
        private String regexPattern;

        /**
         * Current number.
         */
        private int currentValue;

        /**
         * Constructor.
         *
         * @param minValue sets the {@link #minValue}
         * @param maxValue sets the {@link #maxValue}
         */
        public NumberRangePatternComponent(String minValue, String maxValue)
        {
            this.minValue = Integer.valueOf(minValue);
            this.maxValue = Integer.valueOf(maxValue);
            if (this.minValue > this.maxValue) {
                throw new IllegalArgumentException("Min value cannot be greater than max value.");
            }
            this.format = "%0" + Integer.valueOf(minValue.length()).toString() + "d";

            StringBuilder regexPatternBuilder = new StringBuilder();
            regexPatternBuilder.append("\\d{");
            regexPatternBuilder.append(minValue.length());
            regexPatternBuilder.append(",");
            regexPatternBuilder.append(maxValue.length());
            regexPatternBuilder.append("}");
            this.regexPattern = regexPatternBuilder.toString();

            reset();
        }

        @Override
        public void nextComponent()
        {
            currentValue++;
        }

        @Override
        public String getConstant()
        {
            if (currentValue > maxValue) {
                return null;
            }
            return String.format(format, currentValue);
        }

        @Override
        public void reset()
        {
            currentValue = minValue - 1;
        }

        @Override
        public boolean available()
        {
            return currentValue <= maxValue;
        }

        @Override
        public String getRegexPattern()
        {
            return regexPattern;
        }

        @Override
        public boolean isValueValid(String value)
        {
            Integer parsedValue = Integer.valueOf(value);
            return parsedValue >= minValue && parsedValue <= maxValue;
        }
    }

    /**
     * {@link PatternComponent} which returns alphanumeric string of given length
     */
    public static class HashPatternComponent implements GeneratedPatternComponent
    {
        /**
         * Default hash length (if the length isn't explicitly specified).
         */
        public static int DEFAULT_LENGTH = 6;

        /**
         * Length of the string.
         */
        private int length;

        /**
         * Current number.
         */
        private String currentValue;

        /**
         * Constructor.
         */
        public HashPatternComponent(int length)
        {
            if (length <= 0) {
                throw new IllegalArgumentException("Hash length must be greater than zero.");
            }
            this.length = length;
            reset();
        }

        @Override
        public void nextComponent()
        {
            currentValue = RandomStringUtils.randomAlphabetic(1).toLowerCase();
            currentValue += RandomStringUtils.randomAlphanumeric(length - 1).toLowerCase();
        }

        @Override
        public String getConstant()
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

        @Override
        public String getRegexPattern()
        {
            StringBuilder regexPatternBuilder = new StringBuilder();
            regexPatternBuilder.append("\\p{Alpha}");
            regexPatternBuilder.append("[\\p{Alnum}_-]{");
            regexPatternBuilder.append(length - 1);
            regexPatternBuilder.append("}");
            return regexPatternBuilder.toString();
        }

        @Override
        public boolean isValueValid(String value)
        {
            return true;
        }
    }
}
