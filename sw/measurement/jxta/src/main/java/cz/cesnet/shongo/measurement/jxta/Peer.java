package cz.cesnet.shongo.measurement.jxta;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.*;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import org.apache.log4j.Logger;

public class Peer implements PipeMsgListener {

    /** Logger */
    static protected Logger logger = Logger.getLogger(Peer.class);

    /**
     * Attributes
     */
    private String peerName;
    private PeerID peerID;
    private PeerGroup peerGroup;
    private NetworkManager networkManager;
    private Thread serverThread;

    /**
     * Constructor
     *
     * @param peerName
     */
    public Peer(String peerName) {
        this.peerName = peerName;
        this.peerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, peerName.getBytes());
    }


    /**
     * Get peer id
     *
     * @return peer id
     */
    public PeerID getPeerID() {
        return peerID;
    }

    /**
     * Get peer group
     *
     * @return peer group
     */
    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    /**
     * Get pipe advertisement used for sending unicast message
     *
     * @param name  Name of node in net
     * @return advertisement
     */
    public static PipeAdvertisement getAdvertisementUnicastMessage(String name) {
        String pipeName = name + ":UnicastMessagePipe";
        PipeAdvertisement pipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID pipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, pipeName.getBytes());
        pipeAdvertisement.setPipeID(pipeID);
        pipeAdvertisement.setType(PipeService.UnicastType);
        pipeAdvertisement.setName(pipeName);
        return pipeAdvertisement;
    }

    /**
     * Get pipe advertisement used for sending broadcast message
     *
     * @return advertisement
     */
    public static PipeAdvertisement getAdvertisementBroadcastMessage() {
        String pipeName = "BroadcastMessagePipe";
        PipeAdvertisement pipeAdvertisement = (PipeAdvertisement)AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID pipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, pipeName.getBytes());
        pipeAdvertisement.setPipeID(pipeID);
        pipeAdvertisement.setType(PipeService.PropagateType);
        pipeAdvertisement.setName(pipeName);
        return pipeAdvertisement;
    }

    /**
     * Start peer
     *
     * @return void
     */
    public void start() {
        logger.info(String.format("Starting Peer [%s] with ID [%s]", peerName, peerID.toString()));
        try {
            // Creation of network manager
            networkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, peerName, new File(".jxta/" + peerName).toURI());

            // Retrieving the network configurator
            NetworkConfigurator networkConfigurator = networkManager.getConfigurator();
            networkConfigurator.setPeerID(peerID);
            networkConfigurator.setTcpEnabled(true);
            networkConfigurator.setTcpIncoming(true);
            networkConfigurator.setTcpOutgoing(true);
            //networkConfigurator.save();

            // Starting the JXTA network with disabled error stream
            PrintStream err = System.err;
            System.setErr(new java.io.PrintStream(new java.io.OutputStream() { public void write(int b){} }));
            peerGroup = networkManager.startNetwork();
            System.setErr(err);

            // Perform on start event
            onStart();
        } catch ( IOException exception ) {
            // Raised when access to local file and directories caused an error
            logger.error(exception.toString());
        } catch ( PeerGroupException exception ) {
            // Raised when the net peer group could not be created
            logger.error(exception.toString());
        }
    }

    /**
     * Connection wrapper. Once started, it sends ITERATIONS messages and
     * receives a response from the initiator for each message.
     */
    private static class ConnectionHandler implements Runnable {

        private final JxtaBiDiPipe pipe;

        /**
         * Constructor for the ConnectionHandler object
         *
         * @param pipe message pipe
         */
        ConnectionHandler(JxtaBiDiPipe pipe) {
            this.pipe = pipe;
        }

        /** {@inheritDoc} */
        public void run() {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {}
        }
    }

    protected void onStart() {
        // Start server pipe in thread
        final Peer thisPeer = this;
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JxtaServerPipe serverPipe = new JxtaServerPipe(peerGroup, getAdvertisementUnicastMessage(peerName));
                    serverPipe.setPipeTimeout(0);
                    while ( true ) {
                        JxtaBiDiPipe pipe = serverPipe.accept();
                        if ( pipe != null ) {
                            pipe.setMessageListener(thisPeer);

                            Thread thread = new Thread(new ConnectionHandler(pipe));
                            thread.start();
                        } else {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {}
                        }
                    }
                } catch ( java.net.SocketException exception ) {
                    if ( exception.getMessage() != "interrupted" )
                        exception.printStackTrace();
                } catch ( IOException exception ) {
                    exception.printStackTrace();
                }
            }
        });
        serverThread.start();

        // Setup broadcast pipe
        try {
            peerGroup.getPipeService().createInputPipe(getAdvertisementBroadcastMessage(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop peer
     *
     * @return void
     */
    public void stop() {
        // Stop server thread
        if ( serverThread != null ) {
            serverThread.interrupt();
        }

        // Stopping the network
        networkManager.stopNetwork();
    }

    /**
     * Send message to another peer
     *
     * @param receiverName  Peer peerName
     * @param text  Text
     * @return result
     */
    public boolean sendMessage(String receiverName, String text) {
        onSendMessage(receiverName, text);

        try {
            JxtaBiDiPipe pipe = new JxtaBiDiPipe();
            pipe.setReliable(true);
            pipe.connect(peerGroup, getAdvertisementUnicastMessage(receiverName), 10000);

            Message message = new Message();
            message.addMessageElement("default", new StringMessageElement("From", peerName, null));
            message.addMessageElement("default", new StringMessageElement("Text", text, null));

            if ( pipe.sendMessage(message) )
                return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info(String.format("Message could not be delivered to %s: %s", receiverName, text));

        return false;
    }

    /**
     * On send message event
     *
     * @param receiverName
     * @param text
     */
    protected void onSendMessage(String receiverName, String text) {
        logger.info(String.format("Sending message to %s: %s", receiverName, text));
    }

    /**
     * Send message to all peers
     *
     * @param text
     */
    public void sendBroadcastMessage(String text) {
        PipeService pipeService = getPeerGroup().getPipeService();
        try {
            logger.info(String.format("Sending message to all: %s", text));

            Message message = new Message();
            message.addMessageElement("default", new StringMessageElement("From", peerName, null));
            message.addMessageElement("default", new StringMessageElement("Text", text, null));

            OutputPipe outputPipe = pipeService.createOutputPipe(getAdvertisementBroadcastMessage(), 1000);
            if ( outputPipe.send(message) == false )
                logger.info(String.format("Message could not be delivered: %s", text));
            outputPipe.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    public void pipeMsgEvent(PipeMsgEvent pipeMsgEvent) {
        Message message = pipeMsgEvent.getMessage();
        String senderName = message.getMessageElement("default", "From").toString();
        String text = message.getMessageElement("default", "Text").toString();
        onReceiveMessage(senderName, text);
    }

    /**
     * On receive message event
     *
     * @param senderName
     * @param text
     */
    protected void onReceiveMessage(String senderName, String text) {
        logger.info(String.format("Received message from %s: %s", senderName, text));
    }

    /**
     * Static initialization
     */
    static {
        Logging.reconfigure();
    }

}