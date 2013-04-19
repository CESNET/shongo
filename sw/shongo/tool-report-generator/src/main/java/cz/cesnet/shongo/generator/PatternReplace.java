package cz.cesnet.shongo.generator;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Object for replacing string by pattern with {@link Callback}.
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class PatternReplace
{
    /**
     * For implementing replacement determination.
     */
    public static interface Callback
    {
        /**
         * Method called when a {@code matchResult} is found.
         *
         * @param matchResult which have been found
         * @return string by which the {@code matchResult} should be replaced.
         */
        public String callback(MatchResult matchResult);
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
    public PatternReplace(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
    }

    /**
     * @param string source string in which the {@link #pattern} should be replaced.
     * @param callback which should be used for replacing
     * @return replaced string
     */
    public String replace(String string, Callback callback)
    {
        final Matcher matcher = this.pattern.matcher(string);
        int pos = 0;
        while (matcher.find(pos)) {
            final MatchResult matchResult = matcher.toMatchResult();
            final String replacement = callback.callback(matchResult);
            string = string.substring(0, matchResult.start())
                    + replacement + string.substring(matchResult.end());
            pos = matchResult.start() + replacement.length();
            matcher.reset(string);
        }
        return string;
    }
}
