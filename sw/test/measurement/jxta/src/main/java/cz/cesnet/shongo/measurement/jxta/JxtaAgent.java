package cz.cesnet.shongo.measurement.jxta;

import cz.cesnet.shongo.measurement.common.Agent;
import cz.cesnet.shongo.measurement.common.CommandParser;
import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * JxtaAgent implementation for JXTA
 *
 * @author Martin Srom
 */
public class JxtaAgent extends Agent {

    /**
     * JXTA peer
     */
    private Peer peer;

    /**
     * Constructor
     *
     * @param id   Agent id
     * @param name Agent name
     */
    public JxtaAgent(String id, String name) {
        super(id, name);
        this.peer = new Peer(name, this);
    }

    /**
     * Implementation of Fuse agent startup
     *
     * @return result
     */
    @Override
    protected boolean startImpl() {
        return peer.start();
    }

    /**
     * Implementation of Fuse agent finalization
     */
    @Override
    protected void stopImpl() {
        peer.stop();
        System.out.println("Stopped Peer");
    }

    /**
     * Implementation of exit agent
     */
    @Override
    protected void exit() {
        System.exit(0);
    }

    /**
     * Implementation of Fuse agent send message
     *
     * @param receiverName
     * @param message
     */
    @Override
    protected void sendMessageImpl(String receiverName, String message) {
        if ( receiverName.equals("*") )
            peer.sendBroadcastMessage(message);
        else
            peer.sendMessage(receiverName, message);
    }

}
