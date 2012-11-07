package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcClient;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for using the implementation of {@link cz.cesnet.shongo.controller.api.ReservationService} through XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class XmlRpcTypeTest
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
        assertEquals("1", room.getIdentifier());
        assertEquals("room", room.getName());
        assertEquals(5, room.getPortCount());

        room.setPortCount(10);
        room.setOption(Room.Option.PIN, "100");

        getRoomService().modifyRoom(SECURITY_TOKEN, room);
    }

    public interface RoomService extends Service
    {
        @API
        public Room getRoom(SecurityToken token, String roomIdentifier);

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
        public Room getRoom(SecurityToken token, String roomIdentifier)
        {
            assertEquals("1", roomIdentifier);

            Room room = new Room();
            room.setIdentifier("1");
            room.setName("room");
            room.setPortCount(5);
            room.setOption(Room.Option.DESCRIPTION, "room description");
            return room;
        }

        @Override
        public void modifyRoom(SecurityToken token, Room room)
        {
            assertEquals("1", room.getIdentifier());
            assertEquals(10, room.getPortCount());
        }
    }
}
