package cz.cesnet.shongo.util;

import cz.cesnet.shongo.SimplePersistentObject;

import java.util.Collection;

/**
 * Helper class for object equality.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectHelper
{
    /**
     * @param object1
     * @param object2
     * @return true if both objects are same,
     *         false otherwise
     */
    public static boolean isSame(Object object1, Object object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

    /**
     * @param object1
     * @param object2
     * @return true if both objects are same,
     *         false otherwise
     */
    public static boolean isSame(SimplePersistentObject object1, SimplePersistentObject object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return isSame(object1.getId(), object2.getId());
    }

    /**
     * @param object1
     * @param object2
     * @return true if both collections contains same values,
     *         false otherwise
     */
    public static boolean isSame(Collection<?> object1, Collection<?> object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.containsAll(object2);
    }
}
