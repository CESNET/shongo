package cz.cesnet.shongo.controller;

import java.util.*;

/**
 * Enumeration of all possible public object types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ObjectType
{
    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Resource}.
     */
    RESOURCE("res",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.CONTROL_RESOURCE,
                    });
                    put(ObjectRole.RESERVATION, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.RESERVE_RESOURCE
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            null
    ),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.ReservationRequest}.
     */
    RESERVATION_REQUEST("req",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                    put(ObjectRole.RESERVATION_REQUEST_USER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                }},
            null
    ),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Reservation}.
     */
    RESERVATION("rsv",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            null),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    EXECUTABLE("exe",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ, ObjectPermission.WRITE
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }}
            , null),


    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Tag}.
     */
    TAG("tag",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ, ObjectPermission.WRITE
                    });
                    put(ObjectRole.RESERVATION, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.RESERVE_RESOURCE
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            new HashSet<ObjectRole>()
            {{
                    add(ObjectRole.READER);
                    add(ObjectRole.RESERVATION);
            }}),
    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Domain}
     */
    DOMAIN("dom",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ, ObjectPermission.WRITE
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            null),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Resource}.
     */
    FOREIGN_RESOURCES("fres",
            new HashMap<ObjectRole, ObjectPermission[]>()
            {{
                    put(ObjectRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.CONTROL_RESOURCE,
                    });
                    put(ObjectRole.RESERVATION, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.RESERVE_RESOURCE
                    });
                    put(ObjectRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            null
    ),;

    /**
     * Unique code for the {@link ObjectType}.
     */
    private final String code;

    /**
     * Map of all possible {@link ObjectPermission}s for the {@link ObjectType} {@link ObjectRole}.
     */
    private final Map<ObjectRole, Set<ObjectPermission>> roles;

    /**
     * Set of all {@link ObjectRole}s which should be propagated to authorization server.
     */
    private final Set<ObjectRole> propagatableRoles;

    /**
     * Set of all possible {@link ObjectPermission}s for the {@link ObjectType}.
     */
    private final Set<ObjectPermission> permissions;

    /**
     * Constructor.
     *
     * @param code              sets the {@link #code}
     * @param roles             sets the {@link #roles}
     * @param propagatableRoles sets the {@link #propagatableRoles}
     */
    private ObjectType(String code, Map<ObjectRole, ObjectPermission[]> roles, Set<ObjectRole> propagatableRoles)
    {
        this.code = code;
        Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
        Map<ObjectRole, Set<ObjectPermission>> newRoles = new HashMap<ObjectRole, Set<ObjectPermission>>();
        for (Map.Entry<ObjectRole, ObjectPermission[]> role : roles.entrySet()) {
            Set<ObjectPermission> rolePermissions = new HashSet<ObjectPermission>();
            Collections.addAll(permissions, role.getValue());
            Collections.addAll(rolePermissions, role.getValue());
            newRoles.put(role.getKey(), Collections.unmodifiableSet(rolePermissions));
        }
        this.roles = Collections.unmodifiableMap(newRoles);
        this.permissions = Collections.unmodifiableSet(permissions);
        if (propagatableRoles != null) {
            this.propagatableRoles = Collections.unmodifiableSet(propagatableRoles);
        }
        else {
            this.propagatableRoles = null;
        }
    }


    /**
     * @return {@link #code}
     */
    public String getCode()
    {
        return code;
    }

    /**
     * @return sets of allowed {@link ObjectRole}s for the {@link ObjectType}.
     */
    public Set<ObjectRole> getRoles()
    {
        return roles.keySet();
    }

    /**
     * @return ordered list of allowed {@link ObjectRole}s for the {@link ObjectType}.
     */
    public List<ObjectRole> getOrderedRoles()
    {
        List<ObjectRole> orderedRoles = new ArrayList<ObjectRole>(roles.keySet());
        Collections.sort(orderedRoles);
        return orderedRoles;
    }

    /**
     * @return {@link #permissions}
     */
    public Set<ObjectPermission> getPermissions()
    {
        return permissions;
    }

    /**
     * @param role for which the {@link ObjectPermission}s should be returned
     * @return sets of allowed {@link ObjectPermission}s for given {@link ObjectRole} and the {@link ObjectType}.
     */
    public Set<ObjectPermission> getRolePermissions(ObjectRole role)
    {
        return roles.get(role);
    }

    /**
     * @param role to be checked for allowance
     * @return true whether given role is allowed for the {@link ObjectType},
     *         false otherwise
     */
    public boolean allowsRole(ObjectRole role)
    {
        return roles.keySet().contains(role);
    }

    /**
     * @param role to be checked
     * @return true whether given {@code role} for this {@link ObjectType} should be propagated to child entity,
     *         false otherwise
     */
    public boolean isRolePropagatable(ObjectRole role)
    {
        if (propagatableRoles == null) {
            return true;
        }
        return propagatableRoles.contains(role);
    }

    /**
     * Object types by code.
     */
    private static final Map<String, ObjectType> objectTypeByCode = new HashMap<String, ObjectType>();

    /**
     * Static initialization.
     */
    static {
        for (ObjectType objectType : ObjectType.class.getEnumConstants()) {
            objectTypeByCode.put(objectType.getCode(), objectType);
        }
    }

    /**
     * @param code
     * @return {@link ObjectType} with given {@code code} or {@code null} if it doesn't exist
     */
    public static ObjectType getByCode(String code)
    {
        return objectTypeByCode.get(code);
    }
}
