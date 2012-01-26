package cz.cesnet.shongo.measurement.launcher;

public class LauncherInstanceRemote extends LauncherInstance {

    LauncherInstanceRemote(String id, String host) {
        super(id);
    }

    @Override
    public void run(String command) {
        System.out.println("[REMOTE] Run " + getId() + ": " + command);
    }

    @Override
    public void perform(String command) {
        System.out.println("[REMOTE] Perform " + getId() + ": " + command);
    }

    @Override
    public void exit() {
        System.out.println("[REMOTE] Exit " + getId());
    }
}
