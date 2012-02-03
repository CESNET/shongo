package cz.cesnet.shongo.measurement.common;

import java.io.PrintStream;

/**
 * Waiter class that will wait for a specified number of "started" messages to be sent to System.out by agents
 *
 * @author Martin Srom
 */
class StreamMessageWaiter extends PrintStream implements Runnable
{
    private String message;
    private int number;
    private PrintStream outputStream;
    private Thread thread;
    private boolean skipNextNewLine = false;

    public StreamMessageWaiter(String message, int number)
    {
        super(System.out);
        this.outputStream = System.out;
        this.message = message;
        this.number = number;
        System.setOut(this);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) {
        String string = new String(bytes, i, i1);
        if ( string.contains(message) ) {
            number--;
            skipNextNewLine = true;
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

    public void join() throws InterruptedException {
        thread.join();
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
