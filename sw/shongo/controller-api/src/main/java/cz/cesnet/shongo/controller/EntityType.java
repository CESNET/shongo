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
    RESOURCE("res", new HashMap<Role, Permission[]>()
    {{
            put(Role.OWNER, new Permission[]{
                    Permission.READ,
                    Permission.WRITE
            });
        }}),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.ReservationRequest}.
     */
    RESERVATION_REQUEST("req", new HashMap<Role, Permission[]>()
    {{
            put(Role.OWNER, new Permission[]{
                    Permission.READ,
                    Permission.WRITE,
                    Permission.PROVIDE_RESERVATION
            });
            put(Role.RESERVATION_USER, new Permission[]{
                    Permission.READ,
                    Permission.PROVIDE_RESERVATION
            });
        }}),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Reservation}.
     */
    RESERVATION("rsv", new HashMap<Role, Permission[]>()
    {{
            put(Role.OWNER, new Permission[]{
                    Permission.READ,
                    Permission.WRITE,
                    Permission.PROVIDE_RESERVATION
            });
            put(Role.RESERVATION_USER, new Permission[]{
                    Permission.READ,
                    Permission.PROVIDE_RESERVATION
            });
        }}),

    /**
     * Represents a {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    EXECUTABLE("exe", new HashMap<Role, Permission[]>()
    {{
            put(Role.OWNER, new Permission[]{
                    Permission.READ, Permission.WRITE
            });
        }});

    /**
     * Unique code for the {@link EntityType}.
     */
    private String code;

    /**
     * List of {@link Role}s for the {@link EntityType}.
     */
    private Map<Role, Set<Permission>> roles;

    /**
     * Constructor.
     *
     * @param code  sets the {@link #code}
     * @param roles sets the {@link #roles}
     */
    private EntityType(String code, Map<Role, Permission[]> roles)
    {
        this.code = code;
        Map<Role, Set<Permission>> newRoles = new HashMap<Role, Set<Permission>>();
        for (Map.Entry<Role, Permission[]> role : roles.entrySet()) {
            Set<Permission> permissions = new HashSet<Permission>();
            Collections.addAll(permissions, role.getValue());
            newRoles.put(role.getKey(), Collections.unmodifiableSet(permissions));
        }
        this.roles = Collections.unmodifiableMap(newRoles);
    }

    /**
     * @return {@link #code}
     */
    public String getCode()
    {
        return code;
    }

    /**
     * @return sets of allowed {@link Role}s for the {@link EntityType}.
     */
    public Set<Role> getRoles()
    {
        return roles.keySet();
    }

    /**
     * @param role for which the {@link Permission}s should be returned
     * @return sets of allowed {@link Permission}s for given {@link Role} and the {@link EntityType}.
     */
    public Set<Permission> getRolePermissions(Role role)
    {
        return roles.get(role);
    }

    /**
     * @param role to be checked for allowance
     * @return true whether given role is allowed for the {@link EntityType},
     *         false otherwise
     */
    public boolean allowsRole(Role role)
    {
        return roles.keySet().contains(role);
    }
}
