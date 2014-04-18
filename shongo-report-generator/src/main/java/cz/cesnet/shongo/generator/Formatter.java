package cz.cesnet.shongo.generator;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Formatter
{
    public static String formatConstant(String text)
    {
        text = text.replaceAll("[ -]", "_");
        return text.toUpperCase();
    }

    public static String formatCamelCaseFirstLower(String text)
    {
        if (text.equals("class")) {
            text = "className";
        }
        String[] parts = text.split("[ -]");
        StringBuilder camelCase = new StringBuilder();
        for (String part : parts) {
            if (camelCase.length() > 0) {
                camelCase.append(formatFirstUpperCase(part));
            }
            else {
                camelCase.append(formatFirstLowerCase(part));
            }
        }
        return camelCase.toString();
    }

    public static String formatCamelCaseFirstUpper(String text)
    {
        String[] parts = text.split("[ -]");
        StringBuilder camelCase = new StringBuilder();
        for (String part : parts) {
            camelCase.append(formatFirstUpperCase(part));
        }
        return camelCase.toString();
    }

    public static String formatFirstUpperCase(String text)
    {
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(text.substring(0, 1).toUpperCase());
        camelCase.append(text.substring(1));
        return camelCase.toString();
    }

    public static String formatFirstLowerCase(String text)
    {
        StringBuilder camelCase = new StringBuilder();
        camelCase.append(text.substring(0, 1).toLowerCase());
        camelCase.append(text.substring(1));
        return camelCase.toString();
    }

    public static String formatString(String description)
    {
        if (description == null) {
            return null;
        }
        description = description.trim();
        description = description.replaceAll("(?<!\\\\n\\s{0,5})(\\s+)", " ");
        return description;
    }

    public static String splitCamelCase(String text)
    {
        return text.replaceAll(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
    }
}
