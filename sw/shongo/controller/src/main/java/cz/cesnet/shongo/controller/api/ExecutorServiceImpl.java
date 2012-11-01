package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutorServiceImpl extends Component
        implements ExecutorService, Component.EntityManagerFactoryAware, Component.DomainAware,
                   Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

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
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
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
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Executable";
    }

    @Override
    public void deleteExecutable(SecurityToken token, String executableIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long executableId = domain.parseIdentifier(executableIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            ExecutableManager executableManager = new ExecutableManager(entityManager);

            cz.cesnet.shongo.controller.executor.Executable executable = executableManager.get(executableId);

            executableManager.delete(executable);

            entityManager.getTransaction().commit();
        }
        catch (javax.persistence.RollbackException exception) {
            throw new EntityToDeleteIsReferencedException(cz.cesnet.shongo.controller.api.Executable.class, executableId);
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
    public Collection<ExecutableSummary> listExecutables(SecurityToken token)
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        List<cz.cesnet.shongo.controller.executor.Executable> list = executableManager.list();
        List<ExecutableSummary> summaryList = new ArrayList<ExecutableSummary>();
        for (cz.cesnet.shongo.controller.executor.Executable executable : list) {
            ExecutableSummary summary = new ExecutableSummary();
            summary.setIdentifier(domain.formatIdentifier(executable.getId()));
            summary.setSlot(executable.getSlot());
            summary.setState(executable.getState().toApi());
            if ( executable instanceof cz.cesnet.shongo.controller.executor.Compartment ) {
                summary.setType(ExecutableSummary.Type.COMPARTMENT);
            }
            else if ( executable instanceof cz.cesnet.shongo.controller.executor.VirtualRoom ) {
                summary.setType(ExecutableSummary.Type.VIRTUAL_ROOM);
            }
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public cz.cesnet.shongo.controller.api.Executable getExecutable(SecurityToken token, String executableIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long id = domain.parseIdentifier(executableIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        cz.cesnet.shongo.controller.executor.Executable executable = executableManager.get(id);
        cz.cesnet.shongo.controller.api.Executable executableApi = executable.toApi(domain);

        entityManager.close();

        return executableApi;
    }
}
