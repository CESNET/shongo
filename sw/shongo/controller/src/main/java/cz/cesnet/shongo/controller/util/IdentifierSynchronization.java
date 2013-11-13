package cz.cesnet.shongo.controller.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of locks by identifiers.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IdentifierSynchronization
{
    /**
     * Map of locks.
     */
    public Map<Object, Object> locks = new HashMap<Object, Object>();

    /**
     * @param identifier
     * @return lock for given {@code identifier}
     */
    public synchronized Object get(Object identifier)
    {
        Object lock = locks.get(identifier);
        if (lock == null) {
            lock = new Object();
            locks.put(identifier, lock);
        }
        return lock;
    }
}
