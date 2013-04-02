package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.rpc.RpcClient;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import cz.cesnet.shongo.controller.api.rpc.RpcServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

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
        TestingRoomService roomService = new TestingRoomServiceImpl();
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

    public TestingRoomService getRoomService()
    {
        return rpcClient.getService(TestingRoomService.class);
    }

    @Test
    public void test() throws Exception
    {
        TestingRoom room = getRoomService().getRoom(SECURITY_TOKEN, "1");
        Assert.assertEquals("1", room.getId());
        Assert.assertEquals("room", room.getName());
        Assert.assertEquals(5, room.getLicenseCount());

        room.setLicenseCount(10);
        room.setOption(TestingRoomOption.PIN, "100");
        room.setName(null);

        getRoomService().modifyRoom(SECURITY_TOKEN, room);
    }

    public static enum TestingRoomOption
    {
        PIN
    }

    public static class TestingRoom extends IdentifiedChangeableObject implements StructType
    {
        public static final String NAME = "name";

        public static final String LICENSE_COUNT = "licenseCount";

        public static final String OPTIONS = "options";

        public TestingRoom()
        {
        }

        public String getName()
        {
            return getPropertyStorage().getValue(NAME);
        }

        public void setName(String name)
        {
            getPropertyStorage().setValue(NAME, name);
        }

        public int getLicenseCount()
        {
            return getPropertyStorage().getValueAsInt(LICENSE_COUNT);
        }

        public void setLicenseCount(int licenseCount)
        {
            getPropertyStorage().setValue(LICENSE_COUNT, licenseCount);
        }

        public Map<TestingRoomOption, Object> getOptions()
        {
            return getPropertyStorage().getMap(OPTIONS);
        }

        public void setOptions(Map<TestingRoomOption, Object> options)
        {
            getPropertyStorage().setMap(OPTIONS, options);
        }

        public Object getOption(TestingRoomOption option)
        {
            return getPropertyStorage().getMap(OPTIONS).get(option);
        }

        public void setOption(TestingRoomOption option, Object value)
        {
            if (value == null) {
                removeOption(option);
            }
            else {
                getPropertyStorage().addMapItem(OPTIONS, option, value);
            }
        }

        public void removeOption(TestingRoomOption option)
        {
            getPropertyStorage().removeMapItem(OPTIONS, option);
        }
    }

    public interface TestingRoomService extends Service
    {
        @API
        public TestingRoom getRoom(SecurityToken token, String roomId);

        @API
        public void modifyRoom(SecurityToken token, TestingRoom room);
    }

    public class TestingRoomServiceImpl implements TestingRoomService
    {
        @Override
        public String getServiceName()
        {
            return "Room";
        }

        @Override
        public TestingRoom getRoom(SecurityToken token, String roomId)
        {
            Assert.assertEquals("1", roomId);

            TestingRoom room = new TestingRoom();
            room.setId("1");
            room.setName("room");
            room.setLicenseCount(5);
            return room;
        }

        @Override
        public void modifyRoom(SecurityToken token, TestingRoom room)
        {
            Assert.assertEquals("1", room.getId());
            Assert.assertEquals(10, room.getLicenseCount());
            Assert.assertTrue(room.isPropertyItemMarkedAsNew(room.OPTIONS, TestingRoomOption.PIN));
        }
    }
}
