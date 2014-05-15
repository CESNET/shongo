package cz.cesnet.shongo.controller.booking.specification;

import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Specification of a service for some the {@link #executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ExecutableServiceSpecification extends Specification
{
    /**
     * {@link Executable} for which the service should be allocated.
     */
    private Executable executable;

    /**
     * Specifies whether the service should be automatically enabled for the booked time slot.
     */
    private boolean enabled;

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
        modified |= !ObjectHelper.isSamePersistent(getExecutable(), executableServiceSpecification.getExecutable());
        modified |= !ObjectHelper.isSame(isEnabled(), executableServiceSpecification.isEnabled());

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

        String endpointId = executableServiceSpecificationApi.getExecutableId();
        if (endpointId == null) {
            setExecutable(null);
        }
        else {
            Long executableId = ObjectIdentifier.parseId(endpointId, ObjectType.EXECUTABLE);
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            setExecutable(executableManager.get(executableId));
        }

        setEnabled(executableServiceSpecificationApi.isEnabled());
    }
}
