package cz.cesnet.shongo.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

/**
 * Test database
 *
 * @author Martin Srom
 */
public class DatabaseTest
{
    EntityManager entityManager;

    @Before
    public void setUp() throws Exception
    {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("test");
        entityManager = factory.createEntityManager();
    }

    @After
    public void tearDown()
    {
        entityManager.close();
    }

    @Test
    public void test() throws Exception
    {
        entityManager.getTransaction().begin();
        entityManager.persist(new Person("Martin Srom"));
        entityManager.getTransaction().commit();

        List<Person> listPersons = entityManager.createQuery("SELECT p FROM DatabaseTest$Person p").getResultList();
        for (Person person : listPersons) {
            System.out.println(person.toString());
        }
    }

    @Entity
    @Table(name = "person")
    public static class Person
    {
        @Id
        @GeneratedValue
        @Column
        private int id;

        @Column
        private String name;

        public Person()
        {
        }

        public Person(String name)
        {
            super();
            this.name = name;
        }

        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return String.format("Person [id=%d, name=%s]", id, name);
        }
    }
}
