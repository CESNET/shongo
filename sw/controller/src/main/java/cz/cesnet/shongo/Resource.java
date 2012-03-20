package cz.cesnet.shongo;

/**
 * Represents resource
 *
 * @author Martin Srom
 */
public class Resource
{
    private String id;

    private String name;
    
    public Resource(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Resource [" + getId() + ", " + getName() + "]";
    }
}
