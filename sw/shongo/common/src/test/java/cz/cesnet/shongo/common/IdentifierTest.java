package cz.cesnet.shongo.common;

import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * Identifier tests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IdentifierTest
{
    @Test
    public void testFromString() throws Exception
    {
        Identifier identifier = null;

        identifier = new Identifier("shongo:resource:cz.cesnet:8f460144-eefb-4b7c-a4f9-9c74c08a2158");
        assertEquals(Identifier.Type.RESOURCE, identifier.getType());
        assertEquals("cz.cesnet", identifier.getDomain());
        assertEquals(UUID.fromString("8f460144-eefb-4b7c-a4f9-9c74c08a2158"), identifier.getUUID());

        identifier = new Identifier("shongo:reservation:cz.muni.fi:55560144-eefb-4b7c-a4f9-9c74c08a2158");
        assertEquals(Identifier.Type.RESERVATION, identifier.getType());
        assertEquals("cz.muni.fi", identifier.getDomain());
        assertEquals(UUID.fromString("55560144-eefb-4b7c-a4f9-9c74c08a2158"), identifier.getUUID());
    }

    @Test
    public void testToString() throws Exception
    {
        Identifier identifier = null;

        identifier = new Identifier(Identifier.Type.RESOURCE, "cz.cesnet");
        assertEquals("shongo:resource:cz.cesnet:", identifier.toString().substring(0, 26));

        identifier = new Identifier(Identifier.Type.RESERVATION, "cz.muni.fi");
        assertEquals("shongo:reservation:cz.muni.fi:", identifier.toString().substring(0, 30));
    }
}
