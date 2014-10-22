package cz.cesnet.shongo.api;

import java.awt.datatransfer.DataFlavor;

/**
 * Meeting access mode in Adobe Connect.
 *
 * @author opicak <pavelka@cesnet.cz>
 */
public enum AdobeConnectAccessMode
{
    /**
     * Adobe Connect view permission: anyone who has the URL can access. Used for recordings.
     */
    VIEW("view"),

    /**
     * Adobe Connect meeting access mode: anyone who has the URL for the meeting can enter the room. Used for meetings.
     */
    PUBLIC("view-hidden"),

    /**
     * Adobe Connect meeting access mode: only registered users and accepted guests can enter the room.
     */
    PROTECTED("remove"),

    /**
     * Adobe Connect meeting access mode: only registered users and participants can enter.
     */
    PRIVATE("denied");

    private String code;

    private AdobeConnectAccessMode(String code)
    {
        this.code = code;
    }

    /**
     * Returns String code for api call.
     *
     * @return String code, value of permission-id (api call permissions-update)
     */
    public String getPermissionId() {
        return this.code;
    }

}
