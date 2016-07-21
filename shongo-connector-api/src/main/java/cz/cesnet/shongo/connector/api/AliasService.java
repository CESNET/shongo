package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.jade.CommandException;

/**
 * Alias connector API.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface AliasService extends CommonService {
    /**
     * Creates alias for H.323/SIP
     *
     * @param aliasType
     * @param e164Number
     * @return
     * @throws CommandException
     */
    public String createAlias(AliasType aliasType, String e164Number, String roomName) throws CommandException;

    /**
     * Returns formatted alias
     *
     * @param aliasId
     * @return
     * @throws CommandException
     */
    public String getFullAlias(String aliasId) throws CommandException;

    /**
     * Delete alias
     *
     * @param aliasId
     * @return
     * @throws CommandException
     */
    public void deleteAlias(String aliasId) throws CommandException;

    /**
     * Modify existing alias or creates if it does not exits.
     *
     * @param roomName room to be changed
     * @param newRoomName new name, or null
     * @param aliasType for new newE164Number
     * @param e164Number old number to be removed, or null
     * @param newE164Number new number to be added, or null
     * @throws CommandException
     */
    public void modifyAlias(String roomName, String newRoomName, AliasType aliasType, String e164Number,
                            String newE164Number) throws CommandException;
}
