package cz.cesnet.shongo.util;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.SimplePersistentObject;

import java.util.*;

/**
 * Helper class for object equality.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectHelper
{
    /**
     * To be implemented by objects which can be checked if they are same with other objects.
     */
    public static interface SameCheckable
    {
        /**
         * @param object
         * @return whether this object is same as given {@code object},
         * false otherwise
         */
        public boolean isSame(Object object);
    }

    /**
     * @param object1
     * @param object2
     * @return true if both objects are same,
     * false otherwise
     */
    public static boolean isSame(Object object1, Object object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        if (object1 instanceof SameCheckable) {
            SameCheckable sameCheckable = (SameCheckable) object1;
            return sameCheckable.isSame(object2);
        }
        else if (object2 instanceof SameCheckable) {
            SameCheckable sameCheckable = (SameCheckable) object2;
            return sameCheckable.isSame(object1);
        }
        return object1.equals(object2);
    }

    /**
     * @param object1
     * @param object2
     * @return true if both objects are same,
     * false otherwise
     */
    public static boolean isSamePersistent(PersistentObject object1, PersistentObject object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        if (object1 instanceof SameCheckable) {
            SameCheckable sameCheckable = (SameCheckable) object1;
            return sameCheckable.isSame(object2);
        }
        else if (object2 instanceof SameCheckable) {
            SameCheckable sameCheckable = (SameCheckable) object2;
            return sameCheckable.isSame(object1);
        }
        return isSame(object1.getId(), object2.getId());
    }

    /**
     * @param object1
     * @param object2
     * @return true if both collections contains same values, even if in another order,
     * false otherwise
     */
    public static boolean isSameIgnoreOrder(Collection<?> object1, Collection<?> object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        if (object1.size() != object2.size()) {
            return false;
        }
        Iterator iterator1 = object1.iterator();
        while (iterator1.hasNext()) {
            Object item1 = iterator1.next();

            Iterator iterator2 = object2.iterator();
            boolean contains = false;
            while (iterator2.hasNext()) {
                Object item2 = iterator2.next();
                if (item1 == item2) {
                    contains = true;
                    break;
                } else if (item1 != null && item2 != null && isSame(item1, item2)) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param object1
     * @param object2
     * @return true if both collections contains same values in order,
     * false otherwise
     */
    public static boolean isSame(Collection<?> object1, Collection<?> object2)
    {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        if (object1.size() != object2.size()) {
            return false;
        }
        Iterator iterator1 = object1.iterator();
        Iterator iterator2 = object2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            Object item1 = iterator1.next();
            Object item2 = iterator2.next();
            if (item1 == item2) {
                continue;
            }
            else if (item1 != null && item2 != null && !isSame(item1, item2)) {
                return false;
            }
        }
        return true;
    }
}
