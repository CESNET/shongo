package cz.cesnet.shongo.controller;

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
    OWNER("owner"),

    /**
     * This role allows the user to provide a reservation to a new reservation request.
     *
     * @see {@link Permission#PROVIDE_RESERVATION}
     */
    RESERVATION_USER("reservation-user");

    /**
     * Role unique code.
     */
    private String code;

    /**
     * Constructor.
     *
     * @param code sets the {@link #code}
     */
    private Role(String code)
    {
        this.code = code;
    }

    /**
     * @return {@link #code}
     */
    public String getCode()
    {
        return code;
    }
}
