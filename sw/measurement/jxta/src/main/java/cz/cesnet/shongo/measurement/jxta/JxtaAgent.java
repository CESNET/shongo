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

    /** Logger */
    static protected Logger logger = Logger.getLogger(Peer.class);

    /**
     * JXTA peer
     */
    private Peer peer;

    /**
     * Constructor
     *
     * @param id  JxtaAgent id
     * @param name  JxtaAgent name
     */
    public JxtaAgent(String id, String name) {
        super(id, name);
        this.peer = new Peer(name, this);
    }

    @Override
    public void start() {
        peer.start();
    }

    @Override
    public void stop() {
        peer.stop();
        System.out.println("Exited");
        System.exit(0);
    }

    @Override
    public void onSendMessage(String receiverName, String message) {
        super.onSendMessage(receiverName, message);
        if ( receiverName.equals("*") )
            peer.sendBroadcastMessage(message);
        else
            peer.sendMessage(receiverName, message);
    }

}
