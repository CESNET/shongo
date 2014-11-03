package cz.cesnet.shongo.api;

import java.awt.datatransfer.DataFlavor;

/**
 * Permissions for SCO in Adobe Connect.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public enum AdobeConnectPermissions
{
    /**
     * Adobe Connect view permission: anyone who has the URL can access. Used for recordings.
     */
    VIEW("view", false),

    /**
     * Adobe Connect view permission: anyone who has the URL can access. Used for folders.
     */
    VIEW_ONLY("view-only", false),

    /**
     * Adobe Connect meeting access mode: anyone who has the URL for the meeting can enter the room. Used for meetings.
     */
    PUBLIC("view-hidden", true),

    /**
     * Adobe Connect meeting access mode: only registered users and accepted guests can enter the room.
     */
    PROTECTED("remove", true),

    /**
     * Adobe Connect meeting access mode: only registered users and participants can enter.
     */
    PRIVATE("denied", true);

    private String code;

    private boolean usableByMeetings;

    private AdobeConnectPermissions(String code, boolean usableByMeetings)
    {
        this.code = code;
        this.usableByMeetings = usableByMeetings;
    }

    /**
     * Returns String code for api call.
     *
     * @return String code, value of permission-id (api call permissions-update)
     */
    public String getPermissionId() {
        return this.code;
    }

    public boolean isUsableByMeetings() {
        return usableByMeetings;
    }

    public void setUsableByMeetings(boolean usableByMeetings) {
        this.usableByMeetings = usableByMeetings;
    }

    /**
     * Throws {@link }IllegalArgumentException) if permission is not usable by AC meeting.
     */
    public static void checkIfUsableByMeetings(AdobeConnectPermissions permissions) {
        if (permissions == null) {
            return;
        }
        if (!permissions.isUsableByMeetings()) {
            throw new IllegalArgumentException("AC permission: " + permissions.getPermissionId() + " cannot be used as meeting access mode.");
        }
    }

    /**
     * Returns {@link cz.cesnet.shongo.api.AdobeConnectPermissions} by given code.
     * @param code
     * @return AdobeConnectPermissions
     */
    public static AdobeConnectPermissions valueByCode(String code) {
        for (AdobeConnectPermissions value : values()) {
            if (value.getPermissionId().equals(code)) {
                return value;
            }
        }
        return PROTECTED;
    }
}
