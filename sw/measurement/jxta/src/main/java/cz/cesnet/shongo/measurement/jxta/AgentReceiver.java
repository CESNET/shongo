package cz.cesnet.shongo.measurement.jxta;

public class AgentReceiver extends Agent {

    /**
     * Constructor
     *
     * @param id  Agent id
     * @param name   Agent name
     */
    public AgentReceiver(String id, String name) {
        super(id, name);
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
