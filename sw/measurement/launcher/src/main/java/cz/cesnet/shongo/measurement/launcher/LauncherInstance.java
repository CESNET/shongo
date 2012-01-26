package cz.cesnet.shongo.measurement.launcher;

public abstract  class LauncherInstance {
    
    private String id;

    LauncherInstance(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract void run(String command);

    public abstract void perform(String command);

    public abstract void exit();
}
