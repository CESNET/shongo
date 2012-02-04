package cz.cesnet.shongo.measurement.common;

import java.io.PrintStream;

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

    public boolean waitForMessages() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
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
