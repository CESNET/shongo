package cz.cesnet.shongo.measurement.jxta;

import java.util.HashMap;

public class AgentSender extends Agent {

    /**
     * Constructor
     *
     * @param name
     */
    public AgentSender(String name) {
        super(name);
    }

    /**
     * On run agent
     */
    @Override
    public void onRun() {
        super.onRun();
    }

    private HashMap<String, Long> timerMap = new HashMap<String, Long>();

    /**
     * On send message
     *
     * @param receiverName
     * @param text
     */
    @Override
    protected void onSendMessage(String receiverName, String text) {
        timerMap.put(AgentReceiver.getMessageAnswer(text), System.nanoTime());
        super.onSendMessage(receiverName, text);
    }

    /**
     * On receive message
     *
     * @param senderName
     * @param text
     */
    @Override
    protected void onReceiveMessage(String senderName, String text) {
        String durationFormatted = "";
        if ( timerMap.containsKey(text) ) {
            double duration = (double)(System.nanoTime() - timerMap.get(text)) / 1000000.0;
            durationFormatted = String.format(" (in %f ms)", duration);
        }
        logger.info(String.format("Received message from %s: %s%s", senderName, text, durationFormatted));
    }
}
