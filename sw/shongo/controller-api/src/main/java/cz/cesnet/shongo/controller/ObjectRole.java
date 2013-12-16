package cz.cesnet.shongo.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all possible user {@link ObjectRole}s to entities of all {@link EntityType}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ObjectRole
{
    /**
     * User is a owner of an entity (has all permissions).
     */
    OWNER("1"),

    /**
     * This role allows the user to provide a reservation request to a new reservation request.
     *
     * @see {@link ObjectPermission#PROVIDE_RESERVATION_REQUEST}
     */
    RESERVATION_REQUEST_USER("2"),

    /**
     * This role allows the user to read/view entity.
     *
     * @see {@link ObjectPermission#READ}
     */
    READER("3");

    /**
     * Role unique identifier.
     */
    private String id;

    /**
     * Constructor.
     *
     * @param id sets the {@link #id}
     */
    private ObjectRole(String id)
    {
        this.id = id;
    }

    /**
     * @return {@link #id}
     */
    public String getId()
    {
        return id;
    }

    private static Map<String, ObjectRole> roleById;

    static {
        roleById = new HashMap<String, ObjectRole>();
        for (ObjectRole objectRole : ObjectRole.class.getEnumConstants()) {
            roleById.put(objectRole.getId(), objectRole);
        }
    }

    /**
     * @param roleId {@link ObjectRole#id}
     * @return {@link ObjectRole} for given role identifier
     */
    public static ObjectRole forId(String roleId)
    {
        ObjectRole objectRole = roleById.get(roleId);
        if (objectRole == null) {
            throw new IllegalArgumentException();
        }
        return objectRole;
    }
}
