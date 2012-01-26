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
        if ( url.length < 2 ) {
            System.out.println("[REMOTE:" + getId() + "] Bad formatted url for remote '" + host + "'!" + url.length);
            System.out.println("[REMOTE:" + getId() + "] Url should be in format 'hostname:port'.");
            return false;
        }
        String host = url[0];
        int port = Integer.parseInt(url[1]);

        try {
            socket = new Socket(host, port);
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("[REMOTE:" + getId() + "] Unable to connect to " + host + ":" + port + "!");
        }
        if ( socket == null || output == null || input == null )
            return false;

        System.out.println("[REMOTE:" + getId() + "] Run {" + command + "}");
        output.printf("run %s \"%s\"\n", getId(), command);

        return true;
    }

    @Override
    public void perform(String command) {
        System.out.println("[REMOTE:" + getId() + "] Perform {" + command + "}");
        output.printf("perform \"%s\"\n", command);
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
