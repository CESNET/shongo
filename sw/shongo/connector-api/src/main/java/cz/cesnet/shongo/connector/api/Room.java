package cz.cesnet.shongo.connector.api;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room
{
    private RoomUser[] users;
    private boolean allowGuests;
    private int licenseCount;
    private RoomLayout layout;
    private String[] configuration;

    /**
     * @return A flag indicating whether to allow guest users to join the room.
     */
    public boolean isAllowGuests()
    {
        return allowGuests;
    }

    /**
     * @param allowGuests    A flag indicating whether to allow guest users to join the room.
     */
    public void setAllowGuests(boolean allowGuests)
    {
        this.allowGuests = allowGuests;
    }

    /**
     * @return Platform specific configuration.
     */
    public String[] getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration    Platform specific configuration.
     */
    public void setConfiguration(String[] configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @return The default room layout (used for all participants who did not specify a layout of their
    own choice).
     */
    public RoomLayout getLayout()
    {
        return layout;
    }

    /**
     * @param layout    The default room layout (used for all participants who did not specify a layout of their own choice).
     */
    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return Number of licenses that multipoint server can utilize for this room.
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount    Number of licenses that multipoint server can utilize for this room.
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return List of allowed users.
     */
    public RoomUser[] getUsers()
    {
        return users;
    }

    /**
     * @param users    List of allowed users.
     */
    public void setUsers(RoomUser[] users)
    {
        this.users = users;
    }
}
