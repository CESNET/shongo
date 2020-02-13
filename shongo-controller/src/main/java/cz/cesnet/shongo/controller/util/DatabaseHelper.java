package cz.cesnet.shongo.controller.util;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hsqldb.util.DatabaseManagerSwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Database helper.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DatabaseHelper
{
    private static Logger logger = LoggerFactory.getLogger(DatabaseHelper.class);

    /**
     * @param entityManager
     * @return {@link DatabaseManagerSwing}
     */
    public static DatabaseManagerSwing runDatabaseManager(EntityManager entityManager)
    {
        DatabaseManagerSwing databaseManager;
        try {
            databaseManager = new DatabaseManagerSwing()
            {
                @Override
                public void windowClosed(WindowEvent windowEvent)
                {
                    setVisible(false);
                    super.windowClosed(windowEvent);
                }
            };
            databaseManager.main();
        }
        catch (Exception exception) {
            logger.error("Cannot start database manager!", exception);
            return null;
        }

        try {
            Session session = (Session) entityManager.getDelegate();
            SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
            Connection connection = getConnection(sessionFactory);
            databaseManager.connect(connection);
        }
        catch (Exception exception) {
            logger.error("Cannot connect to current database!", exception);
        }
        return databaseManager;
    }

    public static Connection getConnection(SessionFactoryImplementor sessionFactory){
        try {
            return ((SessionImplementor) sessionFactory).getJdbcConnectionAccess()
                    .obtainConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Run database managed and Wait for it to close.
     */
    public static void runDatabaseManagerAndWait(EntityManager entityManager)
    {
        DatabaseManagerSwing databaseManager = runDatabaseManager(entityManager);
        while (databaseManager.isVisible()) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException exception) {
                logger.error("Failed to wait for database manager to close.", exception);
            }
        }
    }

    /**
     * Test HSQLDB.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        logger.debug("Creating entity manager factory...");
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:controller");
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        logger.debug("Entity manager factory created in {} ms.");

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        runDatabaseManagerAndWait(entityManager);
        entityManager.close();
    }
}
