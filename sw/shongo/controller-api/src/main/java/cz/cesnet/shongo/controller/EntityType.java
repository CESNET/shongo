package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TodoImplementException;

import java.util.*;

/**
 * Enumeration of all possible public entity types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum EntityType
{
    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Resource}.
     */
    RESOURCE("res",
            new HashMap<EntityRole, ObjectPermission[]>()
            {{
                    put(EntityRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.CONTROL_RESOURCE,
                    });
                    put(EntityRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            new HashSet<EntityRole>()
            {{
                    add(EntityRole.OWNER);
                }}
    ),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.ReservationRequest}.
     */
    RESERVATION_REQUEST("req",
            new HashMap<EntityRole, ObjectPermission[]>()
            {{
                    put(EntityRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(EntityRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                    put(EntityRole.RESERVATION_REQUEST_USER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                }},
            new HashSet<EntityRole>()
            {{
                    add(EntityRole.OWNER);
                }}
    ),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Reservation}.
     */
    RESERVATION("rsv",
            new HashMap<EntityRole, ObjectPermission[]>()
            {{
                    put(EntityRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ,
                            ObjectPermission.WRITE,
                            ObjectPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(EntityRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }},
            null),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    EXECUTABLE("exe",
            new HashMap<EntityRole, ObjectPermission[]>()
            {{
                    put(EntityRole.OWNER, new ObjectPermission[]{
                            ObjectPermission.READ, ObjectPermission.WRITE
                    });
                    put(EntityRole.READER, new ObjectPermission[]{
                            ObjectPermission.READ
                    });
                }}
            , null);

    /**
     * Unique code for the {@link EntityType}.
     */
    private final String code;

    /**
     * Map of all possible {@link ObjectPermission}s for the {@link EntityType} {@link EntityRole}.
     */
    private final Map<EntityRole, Set<ObjectPermission>> roles;

    /**
     * Set of all {@link EntityRole}s which should be propagated to authorization server.
     */
    private final Set<EntityRole> propagatableRoles;

    /**
     * Set of all possible {@link ObjectPermission}s for the {@link EntityType}.
     */
    private final Set<ObjectPermission> permissions;

    /**
     * Constructor.
     *
     * @param code              sets the {@link #code}
     * @param roles             sets the {@link #roles}
     * @param propagatableRoles sets the {@link #propagatableRoles}
     */
    private EntityType(String code, Map<EntityRole, ObjectPermission[]> roles, Set<EntityRole> propagatableRoles)
    {
        this.code = code;
        Set<ObjectPermission> permissions = new HashSet<ObjectPermission>();
        Map<EntityRole, Set<ObjectPermission>> newRoles = new HashMap<EntityRole, Set<ObjectPermission>>();
        for (Map.Entry<EntityRole, ObjectPermission[]> role : roles.entrySet()) {
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
            this.propagatableRoles = Collections.emptySet();
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
     * @return sets of allowed {@link EntityRole}s for the {@link EntityType}.
     */
    public Set<EntityRole> getRoles()
    {
        return roles.keySet();
    }

    /**
     * @return ordered list of allowed {@link EntityRole}s for the {@link EntityType}.
     */
    public List<EntityRole> getOrderedRoles()
    {
        List<EntityRole> orderedRoles = new ArrayList<EntityRole>(roles.keySet());
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
     * @return sets of allowed {@link ObjectPermission}s for given {@link EntityRole} and the {@link EntityType}.
     */
    public Set<ObjectPermission> getRolePermissions(EntityRole role)
    {
        return roles.get(role);
    }

    /**
     * @param role to be checked for allowance
     * @return true whether given role is allowed for the {@link EntityType},
     *         false otherwise
     */
    public boolean allowsRole(EntityRole role)
    {
        return roles.keySet().contains(role);
    }

    /**
     * @param role to be checked
     * @return true whether given {@code role} for this {@link EntityType} should be propagated to authorization server,
     *         false otherwise
     */
    public boolean isRolePropagatable(EntityRole role)
    {
        return propagatableRoles.contains(role);
    }

    /**
     * Entity types by code.
     */
    private static final Map<String, EntityType> entityTypeByCode = new HashMap<String, EntityType>();

    /**
     * Static initialization.
     */
    static {
        for (EntityType entityType : EntityType.class.getEnumConstants()) {
            entityTypeByCode.put(entityType.getCode(), entityType);
        }
    }

    /**
     * @param code
     * @return {@link EntityType} with given {@code code} or {@code null} if it doesn't exist
     */
    public static EntityType getByCode(String code)
    {
        return entityTypeByCode.get(code);
    }
}
