package cz.cesnet.shongo.measurement.jxta;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class Peer implements PipeMsgListener {

    /** Logger */
    static protected Logger logger = Logger.getLogger(Peer.class);

    /** Attributes */
    private String peerName;
    private PeerID peerID;
    private PeerGroup peerGroup;
    private NetworkManager networkManager;
    private ServerThread serverThread;
    private InputPipe broadCastPipe;

    /** JXTA agent */
    private JxtaAgent agent;

    /**
     * Constructor
     *
     * @param peerName
     */
    public Peer(String peerName, JxtaAgent agent) {
        this.peerName = peerName;
        this.peerID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, peerName.getBytes());
        this.agent = agent;
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
    public boolean start() {
        try {
            String dataPath = ".jxta/" + peerName;
            FileUtils.deleteDirectory(new File(dataPath));

            // Creation of network manager
            networkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, peerName, new File(dataPath).toURI());
            // Automtic stop network on shutdown
            networkManager.registerShutdownHook();

            // Retrieving the network configurator
            NetworkConfigurator networkConfigurator = networkManager.getConfigurator();
            networkConfigurator.setPeerID(peerID);
            networkConfigurator.setUseMulticast(true);
            networkConfigurator.setTcpEnabled(true);
            networkConfigurator.setTcpIncoming(true);
            networkConfigurator.setTcpOutgoing(true);

            // Starting the JXTA network with disabled error stream
            PrintStream err = System.err;
            System.setErr(new java.io.PrintStream(new java.io.OutputStream() { public void write(int b){} }));
            peerGroup = networkManager.startNetwork();
            System.setErr(err);

            logger.info(String.format("Started Peer [%s] with ID [%s]", peerName, peerID.toString()));

            // Perform on start event
            onStart();

            return true;
        } catch ( IOException exception ) {
            // Raised when access to local file and directories caused an error
            logger.error(exception.toString());
        } catch ( PeerGroupException exception ) {
            // Raised when the net peer group could not be created
            logger.error(exception.toString());
        }
        return false;
    }

    /**
     * Server thread class
     */
    private static class ServerThread extends Thread {
        private Peer peer;
        private boolean stopFlag;

        public ServerThread(Peer peer) {
            this.peer = peer;
            this.stopFlag = false;
        }

        public void setStopFlag() {
            this.stopFlag = true;
        }

        @Override
        public void run() {
            List<JxtaBiDiPipe> pipes = new ArrayList<JxtaBiDiPipe>();
            try {
                JxtaServerPipe serverPipe = new JxtaServerPipe(peer.peerGroup, getAdvertisementUnicastMessage(peer.peerName));
                serverPipe.setPipeTimeout(50);
                while ( stopFlag == false ) {
                    try{
                        JxtaBiDiPipe pipe = serverPipe.accept();
                        if ( pipe != null ) {
                            pipe.setMessageListener(peer);
                            pipes.add(pipe);
                        }
                    } catch ( SocketTimeoutException e ) {}
                }
            } catch ( java.net.SocketException exception ) {
                if ( exception.getMessage() != "interrupted" )
                    exception.printStackTrace();
            } catch ( IOException exception ) {
                exception.printStackTrace();
            }

            try {
                for ( JxtaBiDiPipe pipe : pipes )
                    pipe.close();
            } catch (IOException e) {
                System.out.println("closing pipe exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * On start
     */
    protected void onStart() {
        // Start server pipe in thread
        final Peer thisPeer = this;
        serverThread = new ServerThread(this);
        serverThread.start();

        // Setup broadcast pipe
        try {
            broadCastPipe = peerGroup.getPipeService().createInputPipe(getAdvertisementBroadcastMessage(), this);
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
            serverThread.setStopFlag();
            try {
                serverThread.join();
            } catch (InterruptedException e) {}
        }
        broadCastPipe.close();
    }

    /**
     * Send message to another peer
     *
     * @param receiverName  Peer peerName
     * @param text  Text
     * @return result
     */
    public boolean sendMessage(String receiverName, String text) {
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
            if ( e instanceof java.net.SocketTimeoutException ) {
                // OK do nothing
            } else {
                // Print unknown error
                e.printStackTrace();
            }
        }

        logger.info(String.format("Message could not be delivered to %s: %s", receiverName, text));

        return false;
    }

    /**
     * Send message to all peers
     *
     * @param text
     */
    public void sendBroadcastMessage(String text) {
        PipeService pipeService = getPeerGroup().getPipeService();
        try {
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
        agent.onReceiveMessage(senderName, text);
    }

    /**
     * Static initialization
     */
    static {
        Logging.reconfigure();
    }

}