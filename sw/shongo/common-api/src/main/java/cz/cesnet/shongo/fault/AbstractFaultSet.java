package cz.cesnet.shongo.fault;

import java.util.HashMap;
import java.util.Map;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractFaultSet
{
    /**
     * List of {@link Fault} classes bound to fault code.
     */
    private Map<Integer, Class<? extends Fault>> faultClasses;

    /**
     * Add new {@link Fault} class to be bound to a fault code.
     *
     * @param code
     * @param type
     */
    public void addFault(int code, Class<? extends Fault> type)
    {
        faultClasses.put(code, type);
    }

    /**
     * Fill {@link Fault} classes.
     */
    protected void fillFaults()
    {
    }

    /**
     * @return map of {@link Fault} class by fault code
     */
    public final Map<Integer, Class<? extends Fault>> getFaultClasses()
    {
        if (faultClasses == null) {
            faultClasses = new HashMap<Integer, Class<? extends Fault>>();
            fillFaults();
        }
        return faultClasses;
    }

    /**
     * @param code
     * @return {@link Fault} class for given fault {@code code}
     */
    public final Class<? extends Fault> getFaultClass(int code)
    {
        return getFaultClasses().get(code);
    }

    /**
     * Format fault message.
     *
     * @param message
     * @param objects
     */
    public static String formatMessage(String message, Object... objects)
    {
        return String.format(message, evaluateParameters(objects));
    }

    /**
     * Evaluate all given parameters (e.g., classes to class names).
     *
     * @param objects
     * @return array of evaluated parameters
     */
    private static Object[] evaluateParameters(Object[] objects)
    {
        for (int index = 0; index < objects.length; index++) {
            if (objects[index] instanceof Class) {
                objects[index] = getClassShortName((Class) objects[index]);
            }
        }
        return objects;
    }
}
