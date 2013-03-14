package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorServiceImpl extends Component
        implements ExecutorService, Component.EntityManagerFactoryAware,
                   Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Executable";
    }

    @Override
    public void deleteExecutable(SecurityToken token, String executableId) throws FaultException
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.WRITE);

            executableManager.delete(executable);

            entityManager.getTransaction().commit();
        }
        catch (javax.persistence.RollbackException exception) {
            throw new EntityToDeleteIsReferencedException(cz.cesnet.shongo.controller.api.Executable.class,
                    entityId.getPersistenceId());
        }
        catch (FaultException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<ExecutableSummary> listExecutables(SecurityToken token, Map<String, Object> filter)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        String filterUserId = DatabaseFilter.getUserIdFromFilter(filter, userId);
        List<cz.cesnet.shongo.controller.executor.Executable> list = executableManager.list(filterUserId);

        List<ExecutableSummary> summaryList = new ArrayList<ExecutableSummary>();
        for (cz.cesnet.shongo.controller.executor.Executable executable : list) {
            ExecutableSummary summary = new ExecutableSummary();
            summary.setId(EntityIdentifier.formatId(executable));
            summary.setUserId(executable.getUserId());
            summary.setSlot(executable.getSlot());
            summary.setState(executable.getState().toApi());
            if (executable instanceof cz.cesnet.shongo.controller.executor.Compartment) {
                summary.setType(ExecutableSummary.Type.COMPARTMENT);
            }
            else if (executable instanceof RoomEndpoint) {
                summary.setType(ExecutableSummary.Type.VIRTUAL_ROOM);
            }
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable getExecutable(SecurityToken token, String executableId)
            throws FaultException
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            cz.cesnet.shongo.controller.executor.Executable executable = executableManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.READ);

            return executable.toApi();
        }
        finally {
            entityManager.close();
        }
    }
}
