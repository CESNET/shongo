package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.UserInformation;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;

/**
 * Tests for {@link ApacheStorageTest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorageTest
{
    private static final String URL = "TMP";
    //private static final String URL = "/media/shongo_storage";

    private AbstractStorage storage;

    @Before
    public void before() throws Exception
    {
        String rootFolderUrl = URL;
        if (URL.equals("TMP")) {
            File tempFolder = File.createTempFile("storage", null);
            tempFolder.delete();
            tempFolder.mkdir();
            rootFolderUrl = tempFolder.getAbsolutePath();
        }
        ApacheStorage.deleteContentRecursive(new File(rootFolderUrl));

        storage = new ApacheStorage(rootFolderUrl, new AbstractStorage.UserInformationProvider()
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
            ApacheStorage.deleteRecursive(new File(storage.getUrl()));
        }
    }

    @Test
    public void test() throws Exception
    {
        String folderId = storage.createFolder(new Storage.Folder(null, "folder"));
        Assert.assertTrue(dirExists("folder"));

        String recordingId = storage.createFile(new Storage.File(folderId, "recording"), getInputStream("<data>"));
        Assert.assertTrue(fileExists("folder/recording"));
        Assert.assertEquals("<data>", getFileContent("folder/recording"));

        storage.setFolderPermissions(folderId, new HashMap<String, Storage.UserPermission>(){{
            put("srom", Storage.UserPermission.READ);
        }});
        Assert.assertTrue(fileExists("folder/.htaccess"));
        Assert.assertEquals("require user 208213@muni.cz srom@cesnet.cz", getFileContent("folder/.htaccess"));
    }

    private boolean fileExists(String fileName)
    {
        fileName = ApacheStorage.getChildUrl(storage.getUrl(), fileName);
        File file = new File(fileName);
        return file.exists() && file.isFile();
    }

    private boolean dirExists(String dirName)
    {
        dirName = ApacheStorage.getChildUrl(storage.getUrl(), dirName);
        File dir = new File(dirName);
        return dir.exists() && dir.isDirectory();
    }

    private InputStream getInputStream(String data)
    {
        return new ByteArrayInputStream(data.getBytes());
    }

    private String getFileContent(String fileName) throws Exception
    {
        fileName = ApacheStorage.getChildUrl(storage.getUrl(), fileName);
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
}
