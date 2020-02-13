package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Tests for {@link ApacheStorageTest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorageTest
{
    private static Logger logger = LoggerFactory.getLogger(ApacheStorageTest.class);

    private static final String URL = "TMP";

    private AbstractStorage storage;

    @Before
    public void before() throws Exception
    {
        String rootFolderUrl = URL;
        if (URL.equals("TMP")) {
            java.io.File tempFolder = java.io.File.createTempFile("storage", null);
            tempFolder.delete();
            tempFolder.mkdir();
            rootFolderUrl = tempFolder.getAbsolutePath();
        }
        LocalStorageHandler.deleteContentRecursive(new java.io.File(rootFolderUrl));

        storage = new ApacheStorage(rootFolderUrl, "Require user ${userPrincipalName}", new URL("https://shongo-auth-dev.cesnet.cz/tcs/"),
                new AbstractStorage.UserInformationProvider()
                {
                    @Override
                    public UserInformation getUserInformation(String userId)
                    {
                        if (userId.equals("srom")) {
                            UserInformation userInformation = new UserInformation();
                            userInformation.setFirstName("Martin");
                            userInformation.setLastName("Srom");
                            userInformation.addPrincipalName("srom@cesnet.cz");
                            userInformation.addPrincipalName("208213@muni.cz");
                            return userInformation;
                        }
                        else {
                            throw new RuntimeException("User " + userId + " doesn't  exist.");
                        }
                    }
                });
    }

    @After
    public void after() throws Exception
    {
        if (URL.equals("TMP")) {
            LocalStorageHandler.deleteRecursive(new java.io.File(storage.getUrl()));
        }
    }

    @Test
    public void test() throws Exception
    {
        String folderId = storage.createFolder(new Folder(null, "folder"));
        Assert.assertTrue(dirExists("folder"));

        storage.createFile(new File(folderId, "recording"), getInputStream("<data>"));
        String recordingPath  = new java.io.File("folder", "recording").getPath();
        Assert.assertTrue(fileExists(recordingPath));
        Assert.assertEquals("<data>", getFileContent(recordingPath));

        storage.setFolderPermissions(folderId, new HashMap<String, RecordingFolder.UserPermission>()
        {{
                put("srom", RecordingFolder.UserPermission.READ);
            }});
        String htaccessPath = new java.io.File("folder",".htaccess").getPath();
        Assert.assertTrue(fileExists(htaccessPath));
        Assert.assertEquals("Require user 208213@muni.cz\nRequire user srom@cesnet.cz", getFileContent("folder/.htaccess"));
    }

    @Test
    public void testCreateDelete() throws Exception
    {
        String folderId = storage.createFolder(new Folder(null, "folder"));
        Assert.assertTrue(dirExists("folder"));

        File file = new File(folderId, "recording");
        storage.createFile(file, getInputStream("<data>"));
        String recordingPath  = new java.io.File("folder", "recording").getPath();
        Assert.assertTrue(fileExists(recordingPath));
        Assert.assertEquals("<data>", getFileContent("folder/recording"));

        storage.setFolderPermissions(folderId, new HashMap<String, RecordingFolder.UserPermission>()
        {{
                put("srom", RecordingFolder.UserPermission.READ);
            }});

        String htaccessPath = new java.io.File("folder",".htaccess").getPath();
        Assert.assertTrue(fileExists(htaccessPath));
        Assert.assertEquals("Require user 208213@muni.cz\nRequire user srom@cesnet.cz", getFileContent("folder/.htaccess"));

        storage.deleteFile(folderId, "recording");
        Assert.assertFalse(storage.fileExists(file));
    }

    @Test
    public void testConnectionReset() throws Exception
    {
        // Prepare data
        final int size = 1024 * 1024 * 10;
        logger.info("Preparing data...");
        final ByteBuffer fileData = ByteBuffer.allocateDirect(size);
        for (int index = 0; index < fileData.remaining(); index++) {
            fileData.put((byte) (index % 256));
        }
        fileData.rewind();
        logger.info("Data prepared.");

        // Create file
        logger.info("Creating file.");
        final ByteBufferInputStream fileContent = new ByteBufferInputStream(fileData);
        fileContent.setCloseAfterBytes(size / 11);
        final AbstractStorage storage = this.storage;
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                logger.info("Copying started...");
                storage.createFile(new File(null, "test"), fileContent, new ResumeSupport()
                {
                    @Override
                    public InputStream reopenInputStream(InputStream oldInputStream, int offset) throws IOException
                    {
                        ByteBufferInputStream inputStream = new ByteBufferInputStream(fileData);
                        long skipped = inputStream.skip(offset);
                        inputStream.setCloseAfterBytes(size / 5);
                        if (skipped != offset) {
                            throw new RuntimeException("Cannot skip " + offset + ", only skipped " + skipped + ".");
                        }
                        return inputStream;
                    }
                });
                logger.info("Copying finished.");
                super.run();
            }
        };
        thread.start();
        Thread.sleep(10);
        thread.join();
        logger.info("File created.");

        // Check file
        logger.info("Checking file...");
        InputStream inputStream = storage.getFileContent(null, "test");
        try {
            fileData.rewind();
            int position = 0;
            while (inputStream.available() != 0) {
                byte[] expected = new byte[1024];
                byte[] actual = new byte[1024];
                fileData.get(expected);
                inputStream.read(actual);
                Assert.assertEquals("Check at position " + position, new String(expected), new String(actual));
                position += 1024;
            }
        }
        finally {
            inputStream.close();
        }
    }

    /*@Test
    public void testConnectionResetHttp() throws Exception
    {
        final String fileName = "http://195.113.151.184/large_file";
        final HttpClient httpClient = ConfiguredSSLContext.getInstance().createHttpClient();

        HttpGet request = new HttpGet(fileName);
        HttpContext context = new BasicHttpContext();
        HttpResponse response = httpClient.execute(request, context);
        InputStream inputStream = response.getEntity().getContent();
        storage.createFile(new Storage.File(null, "test"), inputStream, new Storage.ResumeSupport()
        {
            @Override
            public InputStream reopenInputStream(InputStream oldInputStream, int offset) throws IOException
            {
                JOptionPane.showMessageDialog(null, "Trying to resume file...");

                HttpGet request = new HttpGet(fileName);
                request.setHeader("Range", "bytes=" + offset + "-");
                HttpContext context = new BasicHttpContext();
                HttpResponse response = httpClient.execute(request, context);
                return response.getEntity().getContent();
            }
        });
    }*/

    private boolean fileExists(String fileName)
    {
        fileName = LocalStorageHandler.getChildPath(storage.getUrl(), fileName);
        java.io.File file = new java.io.File(fileName);
        return file.exists() && file.isFile();
    }

    private boolean dirExists(String dirName)
    {
        dirName = LocalStorageHandler.getChildPath(storage.getUrl(), dirName);
        java.io.File dir = new java.io.File(dirName);
        return dir.exists() && dir.isDirectory();
    }

    private InputStream getInputStream(String data)
    {
        return new ByteArrayInputStream(data.getBytes());
    }

    private String getFileContent(String fileName) throws Exception
    {
        fileName = LocalStorageHandler.getChildPath(storage.getUrl(), fileName);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                line = bufferedReader.readLine();
            }
            return stringBuilder.toString().trim();
        }
        finally {
            bufferedReader.close();
        }
    }

    public class ByteBufferInputStream extends InputStream
    {
        private ByteBuffer byteBuffer;

        private Integer closeAfterBytes;

        public ByteBufferInputStream(ByteBuffer byteBuffer)
        {
            this.byteBuffer = byteBuffer;
            this.byteBuffer.rewind();
        }

        public void setCloseAfterBytes(int closeAfterBytes)
        {
            this.closeAfterBytes = closeAfterBytes;
        }

        public int read() throws IOException
        {
            if (!byteBuffer.hasRemaining()) {
                return -1;
            }
            return byteBuffer.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException
        {
            if (byteBuffer == null) {
                throw new SocketException("Connection reset");
            }
            if (!byteBuffer.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, byteBuffer.remaining());
            byteBuffer.get(bytes, off, len);

            if (closeAfterBytes != null) {
                closeAfterBytes -= len;
                if (closeAfterBytes <= 0) {
                    throw new SocketException("Connection reset");
                }
            }

            return len;
        }

        @Override
        public void close() throws IOException
        {
            byteBuffer = null;
        }
    }
}
