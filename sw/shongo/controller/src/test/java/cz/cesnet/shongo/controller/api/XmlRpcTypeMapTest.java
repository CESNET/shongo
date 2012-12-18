package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcClient;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for serializing changes in {@link java.util.Map} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcTypeMapTest
{
    private static final SecurityToken SECURITY_TOKEN = new SecurityToken();

    private RpcServer rpcServer;

    private RpcClient rpcClient;

    @Before
    public void before() throws Exception
    {
        RoomService roomService = new RoomServiceImpl();
        rpcServer = new RpcServer(null, 8484);
        rpcServer.addHandler(roomService.getServiceName(), roomService);
        rpcServer.start();
        rpcClient = new RpcClient("localhost", 8484);
    }

    @After
    public void after()
    {
        rpcServer.stop();
    }

    public RoomService getRoomService()
    {
        return rpcClient.getService(RoomService.class);
    }

    @Test
    public void test() throws Exception
    {
        Room room = getRoomService().getRoom(SECURITY_TOKEN, "1");
        assertEquals("1", room.getId());
        assertEquals("room", room.getCode());
        assertEquals(5, room.getLicenseCount());

        room.setLicenseCount(10);
        room.setOption(Room.Option.PIN, "100");
        room.setName(null);

        getRoomService().modifyRoom(SECURITY_TOKEN, room);
    }

    public interface RoomService extends Service
    {
        @API
        public Room getRoom(SecurityToken token, String roomId);

        @API
        public void modifyRoom(SecurityToken token, Room room);
    }

    public class RoomServiceImpl implements RoomService
    {
        @Override
        public String getServiceName()
        {
            return "Room";
        }

        @Override
        public Room getRoom(SecurityToken token, String roomId)
        {
            assertEquals("1", roomId);

            Room room = new Room();
            room.setId("1");
            room.setCode("room");
            room.setName("room description");
            room.setLicenseCount(5);
            room.addAlias(new Alias(AliasType.H323_E164, "9501"));
            return room;
        }

        @Override
        public void modifyRoom(SecurityToken token, Room room)
        {
            assertEquals("1", room.getId());
            assertEquals(10, room.getLicenseCount());
            assertEquals(new HashSet<Room.Option>()
            {{
                    add(Room.Option.PIN);
                }}, room.getPropertyItemsMarkedAsNew(Room.OPTIONS));
//            assertEquals(new HashSet<Room.Option>()
//            {{
//                    add(Room.Option.DESCRIPTION);
//                }}, room.getPropertyItemsMarkedAsDeleted(Room.OPTIONS));
        }
    }
}
