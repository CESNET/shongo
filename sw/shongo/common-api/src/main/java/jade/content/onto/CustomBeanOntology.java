package jade.content.onto;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Copy of {@link CustomBeanOntology} with modified {@link #bob}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CustomBeanOntology extends Ontology
{

    private static final long serialVersionUID = -2013125499000302494L;

    private transient CustomBeanOntologyBuilder bob;

    /**
     * Create an Ontology with the given <code>name</code>.
     * The <code>BasicOntology</code> is automatically added
     * as the base ontology.
     *
     * @param name The identifier of the ontology.
     */
    public CustomBeanOntology(String name)
    {
        this(name, BasicOntology.getInstance());
    }

    /**
     * Create an Ontology with the given <code>name</code> that
     * extends the ontology <code>base</code>, which must have
     * <code>BasicOntology</code> in its hierarchy.
     *
     * @param name The identifier of the ontology.
     * @param base The base ontology.
     */
    public CustomBeanOntology(String name, Ontology base)
    {
        this(name, new Ontology[]{base});
    }

    /**
     * Create an Ontology with the given <code>name</code> that
     * extends the <code>base</code> set of ontologies. At least
     * one of the <code>base</code> ontologies must extend the
     * ontology <code>BasicOntology</code>.
     *
     * @param name The identifier of the ontology.
     * @param base The base ontologies
     */
    public CustomBeanOntology(String name, Ontology[] base)
    {
        super(name, base, new BeanIntrospector());
        bob = new CustomBeanOntologyBuilder(this);
    }

    /**
     * Adds to the ontology the schema built from the class <code>clazz</code>.
     * The class must implement either <code>Concept</code>
     * or <code>Predicate</code>.
     *
     * @param clazz class from which to build the ontological schema
     * @throws BeanOntologyException
     */
    public void add(Class clazz) throws BeanOntologyException
    {
        add(clazz, true);
    }

    /**
     * Adds all the ontological beans (the ones which implement either
     * <code>Concept</code> or <code>Predicate</code>) found in the
     * specified package.
     *
     * @param pkgname name of the package containing the beans
     * @throws BeanOntologyException
     */
    public void add(String pkgname) throws BeanOntologyException
    {
        add(pkgname, true);
    }

    /**
     * Adds to the ontology the schema built from the class <code>clazz</code>.
     * The class must implement either <code>Concept</code>
     * or <code>Predicate</code>.
     *
     * @param clazz          class from which to build the ontological schema
     * @param buildHierarchy if <code>true</code>, build the full hierarchy
     *                       ontological elements. Otherwise, build a set of
     *                       flat unrelated elements
     * @throws BeanOntologyException
     */
    public void add(Class clazz, boolean buildHierarchy) throws BeanOntologyException
    {
        bob.addSchema(clazz, buildHierarchy);
    }

    /**
     * Adds all the ontological beans (the ones which implement either
     * <code>Concept</code> or <code>Predicate</code>) found in the
     * specified package.
     *
     * @param pkgname        name of the package containing the beans
     * @param buildHierarchy if <code>true</code>, build the full hierarchy
     *                       ontological elements. Otherwise, build a set of
     *                       flat unrelated elements
     * @throws BeanOntologyException
     */
    public void add(String pkgname, boolean buildHierarchy) throws BeanOntologyException
    {
        bob.addSchemas(pkgname, buildHierarchy);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        // Create a new instance of BOB
        bob = new CustomBeanOntologyBuilder(this);
    }
}
