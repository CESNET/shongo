package cz.cesnet.shongo.controller;

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
            new HashMap<EntityRole, EntityPermission[]>()
            {{
                    put(EntityRole.OWNER, new EntityPermission[]{
                            EntityPermission.READ,
                            EntityPermission.WRITE,
                            EntityPermission.CONTROL_RESOURCE,
                    });
                    put(EntityRole.READER, new EntityPermission[]{
                            EntityPermission.READ
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
            new HashMap<EntityRole, EntityPermission[]>()
            {{
                    put(EntityRole.OWNER, new EntityPermission[]{
                            EntityPermission.READ,
                            EntityPermission.WRITE,
                            EntityPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(EntityRole.READER, new EntityPermission[]{
                            EntityPermission.READ
                    });
                    put(EntityRole.RESERVATION_REQUEST_USER, new EntityPermission[]{
                            EntityPermission.READ,
                            EntityPermission.PROVIDE_RESERVATION_REQUEST
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
            new HashMap<EntityRole, EntityPermission[]>()
            {{
                    put(EntityRole.OWNER, new EntityPermission[]{
                            EntityPermission.READ,
                            EntityPermission.WRITE,
                            EntityPermission.PROVIDE_RESERVATION_REQUEST
                    });
                    put(EntityRole.READER, new EntityPermission[]{
                            EntityPermission.READ
                    });
                }},
            null),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    EXECUTABLE("exe",
            new HashMap<EntityRole, EntityPermission[]>()
            {{
                    put(EntityRole.OWNER, new EntityPermission[]{
                            EntityPermission.READ, EntityPermission.WRITE
                    });
                    put(EntityRole.READER, new EntityPermission[]{
                            EntityPermission.READ
                    });
                }}
            , null);

    /**
     * Unique code for the {@link EntityType}.
     */
    private final String code;

    /**
     * Map of all possible {@link EntityPermission}s for the {@link EntityType} {@link EntityRole}.
     */
    private final Map<EntityRole, Set<EntityPermission>> roles;

    /**
     * Set of all {@link EntityRole}s which should be propagated to authorization server.
     */
    private final Set<EntityRole> propagatableRoles;

    /**
     * Set of all possible {@link EntityPermission}s for the {@link EntityType}.
     */
    private final Set<EntityPermission> permissions;

    /**
     * Constructor.
     *
     * @param code              sets the {@link #code}
     * @param roles             sets the {@link #roles}
     * @param propagatableRoles sets the {@link #propagatableRoles}
     */
    private EntityType(String code, Map<EntityRole, EntityPermission[]> roles, Set<EntityRole> propagatableRoles)
    {
        this.code = code;
        Set<EntityPermission> permissions = new HashSet<EntityPermission>();
        Map<EntityRole, Set<EntityPermission>> newRoles = new HashMap<EntityRole, Set<EntityPermission>>();
        for (Map.Entry<EntityRole, EntityPermission[]> role : roles.entrySet()) {
            Set<EntityPermission> rolePermissions = new HashSet<EntityPermission>();
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
    public Set<EntityPermission> getPermissions()
    {
        return permissions;
    }

    /**
     * @param role for which the {@link EntityPermission}s should be returned
     * @return sets of allowed {@link EntityPermission}s for given {@link EntityRole} and the {@link EntityType}.
     */
    public Set<EntityPermission> getRolePermissions(EntityRole role)
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
}
