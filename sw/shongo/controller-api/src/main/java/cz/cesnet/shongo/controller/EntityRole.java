package cz.cesnet.shongo.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all possible user {@link EntityRole}s to entities of all {@link EntityType}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum EntityRole
{
    /**
     * User is a owner of an entity (has all permissions).
     */
    OWNER("1"),

    /**
     * This role allows the user to provide a reservation request to a new reservation request.
     *
     * @see {@link EntityPermission#PROVIDE_RESERVATION_REQUEST}
     */
    RESERVATION_REQUEST_USER("2"),

    /**
     * This role allows the user to read/view entity.
     *
     * @see {@link EntityPermission#READ}
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
    private EntityRole(String id)
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

    private static Map<String, EntityRole> roleById;

    static {
        roleById = new HashMap<String, EntityRole>();
        for (EntityRole entityRole : EntityRole.class.getEnumConstants()) {
            roleById.put(entityRole.getId(), entityRole);
        }
    }

    /**
     * @param roleId {@link EntityRole#id}
     * @return {@link EntityRole} for given role identifier
     */
    public static EntityRole forId(String roleId)
    {
        EntityRole entityRole = roleById.get(roleId);
        if (entityRole == null) {
            throw new IllegalArgumentException();
        }
        return entityRole;
    }
}
