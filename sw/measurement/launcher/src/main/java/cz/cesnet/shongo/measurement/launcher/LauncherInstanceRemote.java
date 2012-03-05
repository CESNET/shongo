package cz.cesnet.shongo.measurement.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LauncherInstanceRemote extends LauncherInstance {

    private String host;
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;

    LauncherInstanceRemote(String id, String host) {
        super(id);
        this.host = host;
    }

    @Override
    public boolean run(String command) {
        String[] url = host.split(":");
        String host = url[0];
        int port = LauncherApplication.REMOTE_PORT;
        if ( url.length >= 2 )
            port = Integer.parseInt(url[1]);

        try {
            socket = new Socket(host, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("[REMOTE:" + getId() + "] Unable to connect to " + host + ":" + port + "!");
        }
        if ( socket == null || output == null || input == null )
            return false;

        System.out.println("[REMOTE:" + getId() + "] Run [" + command + "]");
        output.printf("run %s \"%s\"\n", getId(), command);
        try {
            String result = input.readLine();
            System.out.println("[REMOTE:" + getId() + "] " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void perform(String command) {
        command = command.replaceAll("\\\"", "\\\\\\\"");
        System.out.println("[REMOTE:" + getId() + "] Perform [" + command + "]");
        output.printf("perform \"%s\"\n", command);
        try {
            String result = input.readLine();
            if ( result == null ) {
                System.err.println("[REMOTE:" + getId() + "] Failed to perform command!");
                return;
            }
            if ( result.equals("[PERFORMED]") == false )
                System.out.println("[REMOTE:" + getId() + "] " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void echo(String value) {
        output.printf("echo \"%s\"\n", value);
    }

    @Override
    public void exit() {
        System.out.println("[REMOTE:" + getId() + "] Exit");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
