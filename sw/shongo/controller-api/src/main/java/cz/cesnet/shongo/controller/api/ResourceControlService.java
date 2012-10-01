package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Interface to the service handling control operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceControlService extends Service
{
    // FIXME: rename target to address - should be address (IP, domain...); add another version of dial using Alias target
    @API
    public String dial(SecurityToken token, String deviceResourceIdentifier, String target) throws FaultException;

    @API
    public String standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException;

    @API
    public String setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level) throws FaultException;

    @API
    public void dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String roomUserId, Alias alias) throws FaultException;
}
