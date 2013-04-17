package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

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
     * @see cz.cesnet.shongo.controller.authorization.Authorization
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
    public void deleteExecutable(SecurityToken token, String executableId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerFaultSet.throwSecurityNotAuthorizedFault("delete executable %s", entityId);
            }

            executableManager.delete(executable, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (javax.persistence.RollbackException exception) {
            ControllerFaultSet.throwEntityNotDeletableReferencedFault(
                    cz.cesnet.shongo.controller.executor.Executable.class, entityId.getPersistenceId());
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Collection<ExecutableSummary> listExecutables(SecurityToken token, Map<String, Object> filter)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        Set<Long> executableIds =
                authorization.getEntitiesWithPermission(userId, EntityType.EXECUTABLE, Permission.READ);
        List<cz.cesnet.shongo.controller.executor.Executable> list = executableManager.list(executableIds);

        List<ExecutableSummary> summaryList = new ArrayList<ExecutableSummary>();
        for (cz.cesnet.shongo.controller.executor.Executable executable : list) {
            ExecutableSummary summary = new ExecutableSummary();
            summary.setId(EntityIdentifier.formatId(executable));
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
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.READ)) {
                ControllerFaultSet.throwSecurityNotAuthorizedFault("read executable %s", entityId);
            }

            Executable executableApi = executable.toApi();
            cz.cesnet.shongo.controller.reservation.Reservation reservation =
                    executableManager.getReservation(executable);
            if (reservation != null) {
                executableApi.setReservationId(EntityIdentifier.formatId(reservation));
            }
            return executableApi;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void updateExecutable(SecurityToken token, String executableId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(executableId, EntityType.EXECUTABLE);

        try {
            entityManager.getTransaction().begin();
            cz.cesnet.shongo.controller.executor.Executable executable =
                    executableManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerFaultSet.throwSecurityNotAuthorizedFault("start executable %s", entityId);
            }

            executable.setNextAttempt(DateTime.now());

            entityManager.getTransaction().commit();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }
}
