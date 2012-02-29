package cz.cesnet.shongo.measurement.launcher;

import cz.cesnet.shongo.measurement.common.Application;
import cz.cesnet.shongo.measurement.common.CommandParser;
import cz.cesnet.shongo.measurement.common.StreamMessageWaiter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class RemoteLauncher {

    private static class ConnectionHandler extends Thread {
        private Socket socket;
        private LauncherInstanceLocal launcherInstance;
        PrintWriter output;
        BufferedReader input;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                output = new PrintWriter(socket.getOutputStream(), true);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if ( output == null || input == null )
                return;

            try {
                String line = null;
                while ( (line = input.readLine()) != null && socket.isConnected() ) {
                    processCommand(line);
                }
                socket.close();
            } catch (IOException e) {
            }

            System.out.println("[REMOTE] Client connection closed");
            if ( launcherInstance != null )
                launcherInstance.exit();
        }

        private void processCommand(String commandToProcess) {
            List<String> list = CommandParser.parse(commandToProcess);
            String command = list.get(0);
            if ( command.equals("run") ) {
                String id = list.get(1);
                String execute = list.get(2);
                System.out.println("[REMOTE] Received for [" + id + "] command run [" + execute + "]");
                launcherInstance = new LauncherInstanceLocal(id);
                StreamMessageWaiter appStartedWaiter = new StreamMessageWaiter(Application.MESSAGE_STARTED,
                        Application.MESSAGE_STARTUP_FAILED);
                appStartedWaiter.startWatching();
                launcherInstance.run(execute);
                if ( appStartedWaiter.waitForMessages() ) {
                    output.println(Application.MESSAGE_STARTED + "[" + id + "]");
                    output.flush();
                }
                else {
                    output.println(Application.MESSAGE_STARTUP_FAILED);
                    output.flush();
                }
                appStartedWaiter.stopWatchingSystem();
            } else if ( command.equals("perform") ) {
                String perform = list.get(1);
                System.out.println("[REMOTE] Received for [" + launcherInstance.getId() + "] command perform [" + perform + "]");
                launcherInstance.perform(perform);
                output.println("[PERFORMED]");
                output.flush();
            } else if ( command.equals("echo") ) {
                launcherInstance.echo(list.get(1));
            }
        }
    }

    public static void launchRemote(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        }
        catch ( IOException e ) {
            System.out.println("Could not listen on port: 4444");
            System.exit(-1);
        }

        System.out.println("[REMOTE] Listening on port " + port + "...");
        while ( true ) {
            try {
                Socket clientSocket = serverSocket.accept();
                if ( clientSocket != null ) {
                    System.out.println("[REMOTE] Accepted client connection");
                    ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
                    connectionHandler.start();
                }
            }
            catch (IOException e) {
                System.out.println("[REMOTE] Accepting client connection failed!");
                System.exit(-1);
            }
        }
    }
}
