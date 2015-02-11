package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.jade.CommandException;

/**
 * Alias connector API.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface AliasService {
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
}
