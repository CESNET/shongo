package cz.cesnet.shongo.controller;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all possible user {@link Role}s to entities of all {@link EntityType}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum Role
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
    private Role(String id)
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

    private static Map<String, Role> roleById;

    static {
        roleById = new HashMap<String, Role>();
        for (Role role : Role.class.getEnumConstants()) {
            roleById.put(role.getId(), role);
        }
    }

    /**
     * @param roleId {@link Role#id}
     * @return {@link Role} for given role identifier
     */
    public static Role forId(String roleId)
    {
        Role role = roleById.get(roleId);
        if (role == null) {
            throw new IllegalArgumentException();
        }
        return role;
    }
}
