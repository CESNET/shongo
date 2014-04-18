package cz.cesnet.shongo.util;

import java.text.Normalizer;

/**
 * Helper for formatting string.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StringHelper
{
    /**
     * @param text         to be formatted
     * @param maxLineWidth maximum line width in the block
     * @return formatted {@code text} to block of {@code maxLineWidth}
     */
    public static String formatBlock(String text, int maxLineWidth)
    {
        if (maxLineWidth == -1) {
            return text;
        }
        StringBuilder stringBuilder = new StringBuilder();
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n");
            }
            while (line.length() > maxLineWidth) {
                int index = line.lastIndexOf(" ", maxLineWidth);
                if (index == -1) {
                    index = maxLineWidth;
                }
                stringBuilder.append(line.substring(0, index));
                stringBuilder.append("\n");
                line = line.substring(index + 1);
            }
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    /**
     * Removing accents and diacritics from given {@code text}.
     *
     * @param text
     * @return given {@code text} without accents and diacritics
     */
    public static String removeAccents(String text)
    {
        if (text == null) {
            return null;
        }
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
