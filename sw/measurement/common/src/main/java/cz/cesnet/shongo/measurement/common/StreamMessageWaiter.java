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

    public StreamMessageWaiter(String message, String messageFailure)
    {
        this(message, messageFailure, 1);
    }

    public StreamMessageWaiter(String message, String messageFailure, int number)
    {
        super(System.out);
        this.outputStream = System.out;
        this.message = message;
        this.messageFailure = messageFailure;
        this.number = number;
        System.setOut(this);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) {
        String string = new String(bytes, i, i1);
        if ( string.contains(message) ) {
            number--;
            processMessage(string);
            super.write(bytes, i, i1);
            //skipNextNewLine = true;
        } else if ( string.contains(messageFailure) ) {
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

    public void start()
    {
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        thread.stop();
    }

    public boolean waitForMessages() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean isMessage(String messageId) {
        return messageIdSet.contains(messageId);
    }

    public boolean isRunning() {
        return number > 0;
    }

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

    @Override
    public void run()
    {
        while ( number > 0 ) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        System.setOut(outputStream);
        Thread.yield();
    }
}
