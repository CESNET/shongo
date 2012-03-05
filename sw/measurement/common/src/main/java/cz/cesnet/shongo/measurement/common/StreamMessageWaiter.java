package cz.cesnet.shongo.measurement.common;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Waiter class that will wait for a specified number of "started" messages to be sent to System.out by agents
 *
 * @author Martin Srom
 */
public class StreamMessageWaiter extends PrintStream implements Runnable
{
    private String message;
    private String messageFailure;
    private int number;
    private PrintStream outputStream;
    private Thread thread;
    private boolean skipNextNewLine = false;
    private boolean result = true;
    private Set<String> messageIdSet = new HashSet<String>();

    /**
     * Constructor
     */
    public StreamMessageWaiter()
    {
        this(null, null, 0);
    }

    /**
     * Constructor
     *
     * @param message Message to watch
     */
    public StreamMessageWaiter(String message)
    {
        this(message, null, 1);
    }

    /**
     * Constructor
     *
     * @param message Message to watch
     * @param messageFailure Failure message to watch
     */
    public StreamMessageWaiter(String message, String messageFailure)
    {
        this(message, messageFailure, 1);
    }

    /**
     * Constructor
     *
     * @param message Message to watch
     * @param number Number of times that message should appear
     */
    public StreamMessageWaiter(String message, int number)
    {
        this(message, null, number);
    }

    /**
     * Constructor
     *
     * @param message Message to watch
     * @param messageFailure Failure message to watch
     * @param number Number of times that message should appear
     */
    public StreamMessageWaiter(String message, String messageFailure, int number)
    {
        super(System.out);
        this.outputStream = System.out;
        System.setOut(this);
        this.set(message, messageFailure, number);
    }

    /**
     * Setup waiter
     *
     * @param message Message to watch
     * @param messageFailure Failure message to watch
     * @param number Number of times that message should appear
     */
    public void set(String message, String messageFailure, int number)
    {
        this.message = message;
        this.messageFailure = messageFailure;
        this.number = number;
    }

    /**
     * Write bytes
     *
     * @param bytes
     * @param i
     * @param i1
     */
    @Override
    public void write(byte[] bytes, int i, int i1) {

        String string = new String(bytes, i, i1);
        if ( message != null && string.contains(message) ) {
            number--;
            processMessage(string);
            super.write(bytes, i, i1);
            //skipNextNewLine = true;
        } else if ( messageFailure != null && string.contains(messageFailure) ) {
            number--;
            result = false;
            super.write(bytes, i, i1);
            //skipNextNewLine = true;
        } else {
            if ( skipNextNewLine ) {
                skipNextNewLine = false;
                if ( string.equals("\n") ) {
                    return;
                }
            }
            super.write(bytes, i, i1);
        }
    }

    /**
     * Start waiter watching for messages
     */
    public void startWatching()
    {
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Stop waiter watching for messages
     */
    public void stopWatching()
    {
        thread.stop();
    }

    /**
     * Wait until all message arrives
     *
     * @return true if none message was failure otherwise false
     */
    public boolean waitForMessages() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Check if concrete message with specified id arrived
     *
     * @param messageId
     * @return
     */
    public boolean isMessage(String messageId) {
        return messageIdSet.contains(messageId);
    }

    /**
     * Check if waiting is running
     *
     * @return
     */
    public boolean isRunning() {
        return number > 0;
    }

    /**
     * Process arrived message
     *
     * @param message
     */
    private void processMessage(String message) {
        String messageId = null;
        int pos = -1;
        // Parse name from message
        if ( (pos = message.indexOf("[REMOTE:")) != -1 ) {
            messageId = message.substring(pos + 8, message.indexOf("]"));
        } else if ( (pos = message.indexOf(": ")) != -1 ) {
            messageId = message.substring(0, pos).trim();
        }
        messageIdSet.add(messageId);
    }

    /**
     * Run watching thread
     */
    @Override
    public void run()
    {
        while ( number > 0 ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        Thread.yield();
    }

    /**
     * Stop waiter watching the System.out
     */
    public void stopWatchingSystem()
    {
        System.setOut(outputStream);
    }

}
