package cz.cesnet.shongo.client.web.models;


import com.google.common.base.Strings;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.Resource;
import org.joda.time.Period;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class ResourceModel
{

    public ResourceModel()
    {
    }

    public ResourceModel(Resource resource)
    {
        if (resource instanceof DeviceResource) {
            this.setType(ResourceType.DEVICE_RESOURCE);
            DeviceResource deviceResource = (DeviceResource) resource;
            this.technologies = new LinkedList<String>();
            this.technologies.add(TechnologyModel.find(deviceResource.getTechnologies()).getTitle());
        } else {
            this.setType(ResourceType.RESOURCE);
        }

        this.id = resource.getId();
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.allocatable = resource.getAllocatable();
        this.calendarPublic = resource.isCalendarPublic();
        this.confirmByOwner = resource.isConfirmByOwner();
        this.administratorEmails = resource.getAdministratorEmails();
        this.capabilities = resource.getCapabilities();
        if (resource.getMaximumFuture() instanceof Period) {
            Period maxFuturePeriod = (Period) resource.getMaximumFuture();
            this.maximumFuture = Integer.toString(maxFuturePeriod.getMonths());
        }
    }

    private String id;

    private String name;

    private String description;

    private ResourceType type;

    private boolean allocatable;

    private boolean calendarPublic;

    private boolean confirmByOwner;

    private String maximumFuture;

    private List<String> technologies;

    private List<String> administratorEmails = new LinkedList<String>();

    private List<Capability> capabilities = new LinkedList<Capability>();

    private String mode;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public void addCapability (Capability capability) {
        this.capabilities.add(capability);
    }

    public void removeCapability (Capability capability) {
        capabilities.remove(capability);
    }

    public void removeCapabilityById (String capabilityId) {
        for (Capability capability: capabilities) {
            if (capabilityId.equals(capability.getId())) {
                capabilities.remove(capability);
                break;
            }
        }
    }

    public List<String> getAdministratorEmails() {
        return administratorEmails;
    }

    public void setAdministratorEmails(List<String> administratorEmails) {
        this.administratorEmails = administratorEmails;
    }

    public void addAdministratorEmail(String administratorEmail) {
        this.administratorEmails.add(administratorEmail);
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public String getMaximumFuture() {
        return maximumFuture;
    }

    public void setMaximumFuture(String maximumFuture) {
        this.maximumFuture = maximumFuture;
    }

    public boolean isConfirmByOwner() {
        return confirmByOwner;
    }

    public void setConfirmByOwner(boolean confirmByOwner) {
        this.confirmByOwner = confirmByOwner;
    }

    public boolean isCalendarPublic() {
        return calendarPublic;
    }

    public void setCalendarPublic(boolean calendarPublic) {
        this.calendarPublic = calendarPublic;
    }

    public boolean isAllocatable() {
        return allocatable;
    }

    public void setAllocatable(boolean allocatable) {
        this.allocatable = allocatable;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Resource toApi () {
        Resource res;
        if (this.type == ResourceType.DEVICE_RESOURCE) {
            res = new DeviceResource();
        } else {
            res = new Resource();
        }

        res.setId(id);
        res.setName(name);
        res.setAllocatable(allocatable);
        res.setDescription(description);
        res.setCalendarPublic(calendarPublic);
        res.setConfirmByOwner(confirmByOwner);
        for (Capability capability : getCapabilities()) {
            res.addCapability(capability);
        }
        for (String email : getAdministratorEmails()) {
            res.addAdministratorEmail(email);
        }
        if (!Strings.isNullOrEmpty(maximumFuture)) {
            Period maxFuturePeriod = new Period().withMonths(Integer.parseInt(maximumFuture));
            res.setMaximumFuture(maxFuturePeriod);
        }

        if (this.type == ResourceType.DEVICE_RESOURCE) {
            if (getTechnologies() != null) {
                DeviceResource deviceResource = (DeviceResource) res;
                Set<Technology> technologySet = new HashSet<Technology>();

                for (String technologySetString : getTechnologies()) {
                    technologySet.addAll(TechnologyModel.getTechnologies(technologySetString));
                }
                for (Technology technology : technologySet) {
                    deviceResource.addTechnology(technology);
                }
            }
        }

        return res;
    }
}
