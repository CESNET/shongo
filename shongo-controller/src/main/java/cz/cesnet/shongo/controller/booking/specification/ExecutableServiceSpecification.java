package cz.cesnet.shongo.controller.booking.specification;

import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Specification of a service for some the {@link #executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ExecutableServiceSpecification extends Specification
        implements ObjectHelper.SameCheckable
{
    /**
     * {@link Resource} where the service should be allocated.
     */
    private Resource resource;

    /**
     * {@link Executable} for which the service should be allocated.
     */
    private Executable executable;

    /**
     * Specifies whether the service should be automatically enabled for the booked time slot.
     */
    private boolean enabled;

    /**
     * @return {@link #resource}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #executable}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }

    /**
     * @return {@link #enabled}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public ExecutableServiceSpecification clone(EntityManager entityManager)
    {
        return (ExecutableServiceSpecification) super.clone(entityManager);
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        ExecutableServiceSpecification executableServiceSpecification = (ExecutableServiceSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSamePersistent(getResource(), executableServiceSpecification.getResource());
        modified |= !ObjectHelper.isSamePersistent(getExecutable(), executableServiceSpecification.getExecutable());
        modified |= !ObjectHelper.isSame(isEnabled(), executableServiceSpecification.isEnabled());

        setResource(executableServiceSpecification.getResource());
        setExecutable(executableServiceSpecification.getExecutable());
        setEnabled(executableServiceSpecification.isEnabled());

        return modified;
    }

    @Override
    public cz.cesnet.shongo.controller.api.ExecutableServiceSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.ExecutableServiceSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        super.toApi(specificationApi);

        cz.cesnet.shongo.controller.api.ExecutableServiceSpecification executableServiceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExecutableServiceSpecification) specificationApi;

        if (resource != null) {
            executableServiceSpecificationApi.setResourceId(ObjectIdentifier.formatId(resource));
        }
        if (executable != null) {
            executableServiceSpecificationApi.setExecutableId(ObjectIdentifier.formatId(executable));
        }
        executableServiceSpecificationApi.setEnabled(enabled);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        super.fromApi(specificationApi, entityManager);

        cz.cesnet.shongo.controller.api.ExecutableServiceSpecification executableServiceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExecutableServiceSpecification) specificationApi;

        String resourceId = executableServiceSpecificationApi.getResourceId();
        if (resourceId == null) {
            setResource(null);
        }
        else {
            Long resourcePersistenceId = ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            setResource(resourceManager.get(resourcePersistenceId));
        }

        String executableId = executableServiceSpecificationApi.getExecutableId();
        if (executableId == null) {
            setExecutable(null);
        }
        else {
            Long executablePersistenceId = ObjectIdentifier.parseLocalId(executableId, ObjectType.EXECUTABLE);
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            setExecutable(executableManager.get(executablePersistenceId));
        }

        setEnabled(executableServiceSpecificationApi.isEnabled());
    }

    @Override
    public boolean isSame(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExecutableServiceSpecification that = (ExecutableServiceSpecification) o;

        if (enabled != that.enabled) return false;
        if (resource != null ? !resource.equals(that.resource) : that.resource != null) return false;
        return !(executable != null ? !executable.equals(that.executable) : that.executable != null);

    }
}
