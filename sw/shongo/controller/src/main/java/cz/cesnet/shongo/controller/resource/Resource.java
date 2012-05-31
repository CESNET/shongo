package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.controller.common.PersistentObject;
import jade.tools.gui.ACLAIDList;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Resource extends PersistentObject
{
    /**
     * Represents a type of a resource.
     */
    public static enum Type
    {
        /**
         * Video/Web Conferencing Equipment
         */
        DEVICE,
        /**
         * Component for Device (e.g., alias, virtual room, etc.)
         */
        DEVICE_COMPONENT,
        /**
         * Physical room (e.g., boardroom, classroom)
         */
        PHYSICAL_ROOM
    }

    /**
     * Unique identifier in whole Shongo.
     */
    private Identifier identifier;

    /**
     * Type of a resource.
     */
    private Type type;

    /**
     * Name of a resource that is visible to users.
     */
    private String name;

    /**
     * Description for a resource.
     */
    private String description;

    /**
     * List of capabilities.
     */
    private List<Capability> capabilities = new ArrayList<Capability>();

    /**
     * Constructor.
     */
    Resource()
    {
    }

    /**
     * @return {@link #identifier} as string
     */
    @Column(name = "identifier")
    public String getIdentifierAsString()
    {
        return (identifier != null ? identifier.toString() : null);
    }

    /**
     * @param identifier Sets the {@link #identifier} from string
     */
    private void setIdentifierAsString(String identifier)
    {
        if (identifier != null) {
            this.identifier = new Identifier(identifier);
        }
        else {
            this.identifier = null;
        }
    }

    /**
     * @return {@link #identifier} object (stored in db as string by IdentifierAsString methods)
     */
    @Transient
    public Identifier getIdentifier()
    {
        return identifier;
    }

    /**
     * Create a new identifier for the resource.
     * @param domain domain to which the resource belongs.
     */
    public void createNewIdentifier(String domain)
    {
        if (identifier != null) {
            throw new IllegalStateException("Resource has already created identifier!");
        }
        identifier = new Identifier(Identifier.Type.RESOURCE, domain);
    }

    /**
     * Change the identifier for the resource.
     * @param domain
     */
    public void changeIdentifier(String domain)
    {
        if (identifier == null) {
            throw new IllegalStateException("Resource hasn't created identifier yet!");
        }
        throw new RuntimeException("TODO: Implement identifier change!");
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #description}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #capabilities}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "resource")
    @Access(AccessType.FIELD)
    public List<Capability> getCapabilities()
    {
        return Collections.unmodifiableList(capabilities);
    }

    /**
     * @param capabilities sets the {@link #capabilities}
     */
    private void setCapabilities(List<Capability> capabilities)
    {
        this.capabilities = capabilities;
    }

    /**
     * @param capability capability to be added to the list of resource capabilities
     */
    public void addCapability(Capability capability)
    {
        capability.setResource(this);
        capabilities.add(capability);
    }

    /**
     * @param map map to which should be filled all parameters
     *            that {@link #toString()} should print.
     */
    protected void fillDescriptionMap(Map<String, String> map)
    {
        map.put("identifier", getIdentifierAsString());
        map.put("type", getType().toString());
        map.put("name", getName());
        map.put("description", getDescription());
    }

    @Override
    public String toString()
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        fillDescriptionMap(map);

        StringBuilder builder = new StringBuilder();

        for ( Map.Entry<String, String> entry : map.entrySet() ) {
            if ( builder.length() > 0 ) {
                builder.append(", ");
            }
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(entry.getValue());
        }
        builder.insert(0, "Resource [");
        builder.append("]");
        return builder.toString();
    }
}
