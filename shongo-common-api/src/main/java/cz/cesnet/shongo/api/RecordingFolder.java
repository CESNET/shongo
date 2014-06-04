package cz.cesnet.shongo.api;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a folder for recordings in multipoint device or endpoint recording server.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingFolder extends IdentifiedComplexType
{
    /**
     * Name of the folder by which the folder and it's recordings can be found in case of manual lookup
     */
    private String name;

    /**
     * Map of {@link UserPermission}s for user-id.
     */
    private Map<String, UserPermission> userPermissions = new HashMap<String, UserPermission>();

    /**
     * Constructor.
     */
    public RecordingFolder()
    {
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    public RecordingFolder(String name)
    {
        this.name = name;
    }

    /**
     * Constructor.
     *
     * @param name sets the {@link #name}
     */
    public RecordingFolder(String name, Map<String, UserPermission> userPermissions)
    {
        this.name = name;
        this.userPermissions = userPermissions;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #userPermissions}
     */
    public Map<String, UserPermission> getUserPermissions()
    {
        return Collections.unmodifiableMap(userPermissions);
    }

    /**
     * @param userPermissions sets the {@link #userPermissions}
     */
    public void setUserPermissions(Map<String, UserPermission> userPermissions)
    {
        this.userPermissions.clear();
        this.userPermissions.putAll(userPermissions);
    }

    /**
     * Add entry to {@link #userPermissions}.
     *
     * @param userId
     * @param userPermission
     */
    public void addUserPermission(String userId, UserPermission userPermission)
    {
        userPermissions.put(userId, userPermission);
    }

    public static final String NAME = "name";
    public static final String USER_PERMISSIONS = "userPermissions";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(USER_PERMISSIONS, userPermissions);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        userPermissions = dataMap.getMap(USER_PERMISSIONS, String.class, UserPermission.class);
    }

    @Override
    public String toString()
    {
        return String.format(RecordingFolder.class.getSimpleName() + " (name: %s, permissions: %s)",
                name, userPermissions);
    }

    /**
     * Permission of user for a {@link Recording}.
     */
    public static enum UserPermission
    {
        READ,
        WRITE
    }
}
