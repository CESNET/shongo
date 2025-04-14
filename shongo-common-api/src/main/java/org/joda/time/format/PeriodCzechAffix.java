package org.joda.time.format;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodCzechAffix implements PeriodFormatterBuilder.PeriodFieldAffix
{
    private final String iSingularText;
    private final String iFewText;
    private final String iPluralText;

    public PeriodCzechAffix(String singularText, String fewText, String pluralText)
    {
        iSingularText = singularText;
        iFewText = fewText;
        iPluralText = pluralText;
    }

    @Override
    public int calculatePrintedLength(int value)
    {
        switch (value) {
            case 1:
                return iSingularText.length();
            case 2:
            case 3:
            case 4:
                return iFewText.length();
            default:
                return iPluralText.length();
        }
    }

    @Override
    public void printTo(StringBuffer buf, int value)
    {
        switch (value) {
            case 1:
                buf.append(iSingularText);
                break;
            case 2:
            case 3:
            case 4:
                buf.append(iFewText);
                break;
            default:
                buf.append(iPluralText);
                break;
        }
    }

    @Override
    public void printTo(Writer out, int value) throws IOException
    {
        switch (value) {
            case 1:
                out.write(iSingularText);
                break;
            case 2:
            case 3:
            case 4:
                out.write(iFewText);
                break;
            default:
                out.write(iPluralText);
                break;
        }
    }

    @Override
    public int parse(String periodStr, int position)
    {
        ArrayList<String> texts = new ArrayList<String>(3);
        texts.add(iSingularText);
        texts.add(iFewText);
        texts.add(iPluralText);
        Collections.sort(texts, new Comparator<String>()
        {
            @Override
            public int compare(String o1, String o2)
            {
                if (o1.length() > o2.length()) {
                    return 1;
                }
                else if (o1.length() < o2.length()) {
                    return -1;
                }
                return o1.compareTo(o2);
            }
        });
        for (String text : texts) {
            if (periodStr.regionMatches
                    (true, position, text, 0, text.length())) {
                return position + text.length();
            }
        }
        return ~position;
    }

    @Override
    public int scan(String periodStr, int position)
    {
        ArrayList<String> texts = new ArrayList<String>(3);
        texts.add(iSingularText);
        texts.add(iFewText);
        texts.add(iPluralText);
        Collections.sort(texts, new Comparator<String>()
        {
            @Override
            public int compare(String o1, String o2)
            {
                if (o1.length() > o2.length()) {
                    return 1;
                }
                else if (o1.length() < o2.length()) {
                    return -1;
                }
                return o1.compareTo(o2);
            }
        });
        for (String text : texts) {
            if (periodStr.regionMatches
                    (true, position, text, 0, text.length())) {
                return position + text.length();
            }
        }
        return ~position;
    }

    @Override
    public String[] getAffixes()
    {
        return new String[] { iSingularText, iFewText, iPluralText };
    }

    @Override
    public void finish(Set<PeriodFormatterBuilder.PeriodFieldAffix> set)
    {
        set.add(this);
    }
}
