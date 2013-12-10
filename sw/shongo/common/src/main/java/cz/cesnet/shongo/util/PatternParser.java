package cz.cesnet.shongo.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object for replacing string by pattern with {@link Callback}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PatternParser
{
    /**
     * For implementing replacement determination.
     */
    public static interface Callback
    {
        /**
         * @param string which was found before/after the match
         * @return string which should be used instead of given
         */
        public String processString(String string);

        /**
         * Method called when a {@code match} is found.
         *
         * @param match which have been found
         * @return string which should be used for the found {@code match}
         */
        public String processMatch(MatchResult match);
    }

    /**
     * Compiled {@link Pattern}.
     */
    private final Pattern pattern;

    /**
     * Constructor.
     *
     * @param pattern for replacing
     */
    public PatternParser(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
    }

    /**
     * Constructor.
     *
     * @param pattern sets the {@link #pattern}
     */
    public PatternParser(Pattern pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @param text     source string in which the {@link #pattern} should be replaced.
     * @param callback to be called for each parsed value
     * @return replaced string parts
     */
    public Collection<String> parse(String text, Callback callback)
    {
        Collection<String> textParts = new LinkedList<String>();
        Matcher matcher = this.pattern.matcher(text);
        while (matcher.find()) {
            MatchResult matchResult = matcher.toMatchResult();
            if (matchResult.start() > 0) {
                textParts.add(callback.processString(text.substring(0, matchResult.start())));
            }
            textParts.add(callback.processMatch(matchResult));
            text = text.substring(matchResult.end());
            matcher.reset(text);
        }
        if (!text.isEmpty()) {
            textParts.add(callback.processString(text));
        }
        return textParts;
    }

    /**
     * @param text     source string in which the {@link #pattern} should be replaced.
     * @param callback to be called for each parsed value
     * @return replaced string
     */
    public String parseAndJoin(String text, Callback callback)
    {
        StringBuilder result = new StringBuilder();
        for (String part : parse(text, callback)) {
            result.append(part);
        }
        return result.toString();
    }
}
