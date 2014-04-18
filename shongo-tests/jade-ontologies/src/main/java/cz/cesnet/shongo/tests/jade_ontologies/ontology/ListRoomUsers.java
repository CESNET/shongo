package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.AgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ListRoomUsers implements AgentAction
{
    SecurityToken token;
    String roomId;

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public SecurityToken getToken()
    {
        return token;
    }

    public void setToken(SecurityToken token)
    {
        this.token = token;
    }
}
