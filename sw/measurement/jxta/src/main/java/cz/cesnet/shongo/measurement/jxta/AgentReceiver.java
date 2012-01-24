package cz.cesnet.shongo.measurement.jxta;

public class AgentReceiver extends Agent {

    /**
     * Constructor
     *
     * @param name
     */
    public AgentReceiver(String name) {
        super(name);
    }

    /**
     * On run agent
     */
    @Override
    public void onRun() {
        super.onRun();
    }


    /**
     * Get message answer
     *
     * @param message Original message
     * @return answer
     */
    public static String getMessageAnswer(String message) {
        return "answer to [" + message + "]";
    }

    /**
     * On receive message
     *
     * @param senderName
     * @param text
     */
    @Override
    protected void onReceiveMessage(String senderName, String text) {
        super.onReceiveMessage(senderName, text);
        sendMessage(senderName, getMessageAnswer(text));
    }
}
