package cz.cesnet.shongo.controller;

/**
 * Enumeration of all possible user permissions for entities of all {@link ObjectType}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ObjectPermission
{
    /**
     * User can read/view the entity.
     */
    READ("read"),

    /**
     * User can write/modify/delete the entity and also the user can assign all permissions for
     * other users to the entity.
     */
    WRITE("write"),

    /**
     * This permission allows the user to provide a reservation request to a new reservation request.
     * This permission can be set only for reservation request entities.
     */
    PROVIDE_RESERVATION_REQUEST("provide-reservation-request"),

    /**
     * Control device resource.
     */
    CONTROL_RESOURCE("control-resource");

    /**
     * Permission unique code.
     */
    private String code;

    /**
     * Constructor.
     *
     * @param code sets the {@link #code}
     */
    private ObjectPermission(String code)
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
