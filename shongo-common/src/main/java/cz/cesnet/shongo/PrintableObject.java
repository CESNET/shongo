package cz.cesnet.shongo;

import java.util.*;

/**
 * Represents an object that can print attributes defined in {@link #fillDescriptionMap(java.util.Map)} by
 * {@link #toString()} method.
 * <p/>
 * Class also contains static method {@link #toString(Object)} for printing non-extending objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class PrintableObject
{
    /**
     * @param map map to which should be filled all parameters
     *            that {@link #toString()} should print.
     */
    protected void fillDescriptionMap(Map<String, Object> map)
    {
    }

    @Override
    public String toString()
    {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        fillDescriptionMap(map);

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", \n");
            }
            builder.append("  ");
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(formatChild(toString(entry.getValue())));
        }
        builder.insert(0, getClass().getSimpleName() + " {\n");
        builder.append("\n}");
        return builder.toString();
    }

    /**
     * @param object
     * @return given object formatted as string
     */
    public static String toString(Object object)
    {
        if (object instanceof Map) {
            Map map = (Map) object;
            StringBuilder builder = new StringBuilder();
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (builder.length() > 0) {
                    builder.append(", \n");
                }
                builder.append("  ");
                builder.append(formatChild(toString(key)));
                builder.append(" => ");
                builder.append(formatChild(toString(value)));
            }
            builder.insert(0, "{\n");
            builder.append("\n}");
            return builder.toString();
        }
        else if (object instanceof List) {
            List list = (List) object;
            return formatCollection(list);
        }
        else if (object instanceof Object[]) {
            return formatCollection(Arrays.asList((Object[]) object));
        }
        return (object == null ? "null" : object.toString());
    }

    /**
     * Print given {@code object} to {@link System#out}.
     *
     * @param object
     */
    public static void printToSystemOut(Object object)
    {
        System.out.println(toString(object));
    }

    /**
     * @param child
     * @return formatted child string
     */
    private static String formatChild(String child)
    {
        if (child == null) {
            return "null";
        }
        else {
            return child.replace("\n", "\n  ");
        }
    }

    /**
     * @param collection
     * @return formatted collection
     */
    protected static String formatCollection(Collection collection)
    {
        if (collection.size() > 0) {
            StringBuilder builder = new StringBuilder();
            boolean multiline = false;
            for (Object object : collection) {
                if (builder.length() > 0) {
                    builder.append(", ");
                    if (multiline) {
                        builder.append("\n");
                    }
                }
                String objectString = toString(object);
                builder.append(objectString);
                multiline = multiline || (objectString.indexOf("\n") != -1);
            }
            if (multiline) {
                return "[\n  " + builder.toString().replace("\n", "\n  ") + "\n]";
            }
            else {
                return "[" + builder.toString() + "]";
            }
        }
        return "[]";
    }
}
