package cz.cesnet.shongo.common;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an object that can be persisted to a database.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@MappedSuperclass
public abstract class PersistentObject
{
    /**
     * Persistent object must have an unique identifier.
     */
    private Long id;

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return true if object has already been persisted, otherwise false
     */
    @Transient
    public boolean isPersisted()
    {
        return id != null;
    }

    /**
     * Checks whether object has already been persisted.
     * @throws IllegalStateException
     */
    public void checkPersisted() throws IllegalStateException
    {
        if (isPersisted() == false) {
            throw new IllegalArgumentException(this.getClass().getSimpleName() + " hasn't been persisted yet!");
        }
    }

    /**
     * @param map map to which should be filled all parameters
     *            that {@link #toString()} should print.
     */
    protected void fillDescriptionMap(Map<String, String> map)
    {
        if (getId() != null) {
            map.put("id", getId().toString());
        }
    }

    /**
     * Add collection to map.
     *
     * @param map
     * @param name
     * @param collection
     */
    protected static void addCollectionToMap(Map<String, String> map, String name, Collection collection)
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
                String objectString = object.toString();
                builder.append(objectString);
                multiline = multiline || (objectString.indexOf("\n") != -1);
            }
            if (multiline) {
                map.put(name, "[\n  " + builder.toString().replace("\n", "\n  ") + "\n]");
            }
            else {
                map.put(name, "[" + builder.toString() + "]");
            }
        }
    }

    @Override
    public String toString()
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        fillDescriptionMap(map);

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", \n");
            }
            builder.append("  ");
            builder.append(entry.getKey());
            builder.append("=");
            if (entry.getValue() == null) {
                builder.append("null");
            }
            else {
                builder.append(entry.getValue().replace("\n", "\n  "));
            }
        }
        builder.insert(0, getClass().getSimpleName() + " {\n");
        builder.append("\n}");
        return builder.toString();
    }
}
