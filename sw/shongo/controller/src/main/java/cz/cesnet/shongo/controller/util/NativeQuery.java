package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.TodoImplementException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.ejb.EntityManagerFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider for native queries from resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NativeQuery
{
    private static Logger logger = LoggerFactory.getLogger(NativeQuery.class);

    /**
     * Native queries.
     */
    public static final String INIT = "init.sql";
    public static final String RESERVATION_REQUEST_LIST = "reservation_request_list.sql";
    public static final String RESERVATION_REQUEST_HISTORY = "reservation_request_history.sql";

    /**
     * Cached native queries (not targeted for any specific {@link EntityManagerFactory}).
     */
    private static Map<String, String> cachedNativeQuery = new HashMap<String, String>();

    /**
     * Cached native queries for specific {@link EntityManagerFactory}.
     */
    private static Map<EntityManagerFactory, Map<String, String>> cachedNativeQueryByEntityManagerFactory =
            new HashMap<EntityManagerFactory, Map<String, String>>();

    /**
     * @param fileName
     * @return native query
     */
    public static synchronized String getNativeQuery(String fileName)
    {
        String nativeQuery = cachedNativeQuery.get(fileName);
        if (nativeQuery != null) {
            return nativeQuery;
        }
        nativeQuery = loadNativeQuery("sql/" + fileName);
        cachedNativeQuery.put(fileName, nativeQuery);
        return nativeQuery;
    }

    /**
     * @param entityManager
     * @param fileName
     * @return native query
     */
    public static synchronized String getNativeQuery(EntityManager entityManager, String fileName)
    {
        EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
        return getNativeQuery(entityManagerFactory, fileName);
    }

    /**
     * @param entityManagerFactory
     * @param fileName
     * @return native query
     */
    public static synchronized String getNativeQuery(EntityManagerFactory entityManagerFactory, String fileName)
    {
        // Try to read SQL from cache
        Map<String, String> cachedNativeQueryForEntityManagerFactory = cachedNativeQueryByEntityManagerFactory.get(entityManagerFactory);
        if (cachedNativeQueryForEntityManagerFactory == null) {
            cachedNativeQueryForEntityManagerFactory = new HashMap<String, String>();
            cachedNativeQueryByEntityManagerFactory.put(entityManagerFactory, cachedNativeQueryForEntityManagerFactory);
        }
        String nativeQuery = cachedNativeQueryForEntityManagerFactory.get(fileName);
        if (nativeQuery != null) {
            return nativeQuery;
        }

        // Determine SQL directory
        if (!(entityManagerFactory instanceof EntityManagerFactoryImpl)) {
            throw new TodoImplementException(entityManagerFactory.getClass());
        }
        EntityManagerFactoryImpl entityManagerFactoryImpl = (EntityManagerFactoryImpl) entityManagerFactory;
        Dialect dialect = entityManagerFactoryImpl.getSessionFactory().getDialect();
        String directory;
        if (dialect instanceof HSQLDialect) {
            directory = "hsqldb";
        }
        else if (dialect instanceof PostgreSQL81Dialect) {
            directory = "postgresql";
        }
        else {
            throw new TodoImplementException(dialect.getClass());
        }

        nativeQuery = loadNativeQuery("sql/" + directory + "/" + fileName);

        // Add SQL to cache
        cachedNativeQueryForEntityManagerFactory.put(fileName, nativeQuery);
        return nativeQuery;
    }

    /**
     * Load given {@code nativeQuery}.
     *
     * @param fileName
     * @return native query
     */
    private static String loadNativeQuery(String fileName)
    {
        logger.debug("Loading native SQL {}...", fileName);

        // Read SQL from proper file
        InputStream inputStream = NativeQuery.class.getClassLoader().getResourceAsStream(fileName);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder sqlBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                line = line.replaceAll("/\\*.*?\\*/", "");
                line = line.replaceAll("\\s*/\\*.*", "");
                line = line.replaceAll("\\s*\\*.*", "");
                if (!line.isEmpty()) {
                    if (line.charAt(0) == 65279) {
                        line = line.substring(1);
                    }
                    if (!line.isEmpty()) {
                        sqlBuilder.append(line);
                        sqlBuilder.append("\n");
                    }
                }
                line = bufferedReader.readLine();
            }
            return sqlBuilder.toString();
        }
        catch (IOException exception) {
            throw new RuntimeException("Failed to load native SQL " + fileName + ".", exception);
        }
    }

    /**
     * Execute update query (or multiple queries delimited by ";").
     *
     * @param entityManager
     * @param nativeQuery
     */
    public static void executeNativeUpdate(EntityManager entityManager, String nativeQuery)
    {
        entityManager.getTransaction().begin();
        for (String statement : nativeQuery.split(";")) {
            statement = statement.trim();
            if (statement.isEmpty()) {
                continue;
            }
            entityManager.createNativeQuery(statement);
        }
        entityManager.getTransaction().commit();
    }
}
