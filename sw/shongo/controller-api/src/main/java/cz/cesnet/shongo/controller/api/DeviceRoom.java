package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Executable} for a virtual room in a {@link DeviceResource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceRoom extends Executable
{
    /**
     * Identifier of the {@link Resource}.
     */
    private String resourceIdentifier;

    /**
     * Number of available ports in the {@link DeviceRoom}.
     */
    private int licenseCount;

    /**
     * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link DeviceRoom}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #resourceIdentifier}
     */
    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    /**
     * @param resourceIdentifier sets the {@link #resourceIdentifier}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliases sets the {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }
}
