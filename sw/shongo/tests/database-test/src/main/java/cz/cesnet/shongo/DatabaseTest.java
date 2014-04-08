package cz.cesnet.shongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DatabaseTest
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("\nInitializing HSQLDB\n");
        EntityManagerFactory entityManagerFactoryHsqldb = Persistence.createEntityManagerFactory("hsqldb");
        System.out.println("\nTesting HSQLDB\n");
        test(entityManagerFactoryHsqldb);

        System.out.println("\nInitializing PostgreSQL\n");
        EntityManagerFactory entityManagerFactoryPostgres = Persistence.createEntityManagerFactory("postgres");
        System.out.println("\nTesting PostgreSQL\n");
        test(entityManagerFactoryPostgres);

        System.out.println("\nCleaning\n");

        EntityManager entityManager = entityManagerFactoryPostgres.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DROP SCHEMA public CASCADE;").executeUpdate();
        entityManager.createNativeQuery("CREATE SCHEMA public;").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    private static void test(EntityManagerFactory entityManagerFactory)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        for (int parent = 0; parent < 1000; parent++) {
            TestEntity testEntity = new TestEntity();
            testEntity.setAttribute("test");
            for (int child = 0; child < 10; child++) {
                TestChildEntity testChildEntity = new TestChildEntity();
                testChildEntity.setAttribute("test");
                testEntity.addChildEntity(testChildEntity);
            }

            entityManager.persist(testEntity);
        }

        entityManager.getTransaction().commit();

        List<TestEntity> entityList = entityManager.createQuery(
                "SELECT entity from TestEntity entity", TestEntity.class)
                .getResultList();

        for (TestEntity entity : entityList) {
            entity.getChildEntities();
        }
    }
}
