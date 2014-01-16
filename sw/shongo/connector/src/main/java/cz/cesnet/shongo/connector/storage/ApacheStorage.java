package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;

/**
 * {@link AbstractStorage} which is implemented by local folder which is published by Apache.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorage extends AbstractStorage
{
    /**
     * Constructor.
     *
     * @param url                     sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    protected ApacheStorage(String url, UserInformationProvider userInformationProvider)
    {
        super(url, userInformationProvider);
    }

    @Override
    public String createFolder(Folder folder)
    {
        String folderId = getChildId(folder.getParentFolderId(), folder.getFolderName());
        String folderUrl = getUrlFromId(folderId);
        java.io.File file = new java.io.File(folderUrl);
        if (!file.mkdir()) {
            throw new RuntimeException("Directory '" + folderUrl + "' already exists.");
        }
        return folderId;
    }

    @Override
    public void deleteFolder(String folderId)
    {
        String folderUrl = getUrlFromId(folderId);
        if (!deleteRecursive(new java.io.File(folderUrl))) {
            throw new RuntimeException("Directory '" + folderUrl + "' couldn't be deleted.");
        }
    }

    @Override
    public List<Folder> listFolders(String folderId, final String folderName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioParentFolder = new java.io.File(folderUrl);
        String[] ioFolders = ioParentFolder.list(new java.io.FilenameFilter()
        {
            @Override
            public boolean accept(java.io.File directory, String fileName)
            {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isDirectory() || fileName.equals(".") || fileName.equals("..")) {
                    return false;
                }
                return folderName == null || StringUtils.containsIgnoreCase(fileName, folderName);
            }
        });
        List<Folder> folders = new LinkedList<Folder>();
        for (String ioFolder : ioFolders) {
            Folder folder = new Folder();
            folder.setFolderId(getChildId(folderId, ioFolder));
            folder.setParentFolderId(folderId);
            folder.setFolderName(ioFolder);
            folders.add(folder);
        }
        return folders;
    }

    @Override
    public void setFolderPermissions(String folderId, Map<String, UserPermission> userPermissions)
    {
        String folderUrl = getUrlFromId(folderId);
        String permissionFileUrl = getChildUrl(folderUrl, ".htaccess");

        StringBuilder permissionFileContent = new StringBuilder();
        permissionFileContent.append("require user");
        for (String userId : new TreeSet<String>(userPermissions.keySet())) {
            UserInformation userInformation = getUserInformation(userId);
            for (String principalName : new TreeSet<String>(userInformation.getPrincipalNames())) {
                permissionFileContent.append(" ");
                permissionFileContent.append(principalName);
            }
        }
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(permissionFileUrl));
            out.print(permissionFileContent);
        }
        catch (FileNotFoundException exception) {
            throw new RuntimeException("File '" + permissionFileUrl + "' couldn't be written.", exception);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public String createFile(File file, InputStream fileContent)
    {
        String folderUrl = getUrlFromId(file.getFolderId());
        String fileUrl = getChildUrl(folderUrl, file.getFileName());
        if (new java.io.File(fileUrl).exists()) {
            throw new RuntimeException("File '" + fileUrl + "' already exists.");
        }
        try {
            OutputStream fileOutputStream = new FileOutputStream(fileUrl);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileContent.close();
            fileOutputStream.close();
        }
        catch (IOException exception) {
            throw new RuntimeException("File '" + fileUrl + "' couldn't be written.", exception);
        }
        return file.getFileName();
    }

    @Override
    public void deleteFile(String folderId, String fileId)
    {
        String folderUrl = getUrlFromId(folderId);
        String fileUrl = getChildUrl(folderUrl, fileId);
        java.io.File file = new java.io.File(fileUrl);
        if (!file.delete()) {
            throw new RuntimeException("File'" + fileUrl + "' couldn't be deleted.");
        }
    }

    @Override
    public List<File> listFiles(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioFolder = new java.io.File(folderUrl);
        String[] ioFiles = ioFolder.list(new java.io.FilenameFilter()
        {
            @Override
            public boolean accept(java.io.File directory, String fileName)
            {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isFile()) {
                    return false;
                }
                return fileName == null || StringUtils.containsIgnoreCase(fileName, fileName);
            }
        });
        List<File> files = new LinkedList<File>();
        for (String ioFile : ioFiles) {
            File file = new File();
            file.setFileId(ioFile);
            file.setFolderId(folderId);
            file.setFileName(ioFile);
            files.add(file);
        }
        return files;
    }

    @Override
    public InputStream getFileContent(String folderId, String fileId)
    {
        throw new TodoImplementException("ApacheStorage.getFileContent");
    }

    private String getUrlFromId(String id)
    {
        if (id == null) {
            return getUrl();
        }
        else {
            return getChildUrl(getUrl(), id);
        }
    }

    private static String getChildId(String parentId, String childName)
    {
        if (parentId == null) {
            return childName;
        }
        else {
            return parentId + "/" + childName;
        }
    }


    static String getChildUrl(String parentUrl, String childName)
    {
        StringBuilder childUrl = new StringBuilder();
        childUrl.append(parentUrl);
        if (childUrl.charAt(childUrl.length() - 1) != '/') {
            childUrl.append("/");
        }
        childUrl.append(childName);
        return childUrl.toString();
    }

    /**
     * Delete file or folder recursive.
     *
     * @param file file or folder to be deleted
     * @return true whether deletion succeeded, false otherwise
     */
    public static boolean deleteRecursive(java.io.File file)
    {
        deleteContentRecursive(file);
        return file.delete();
    }

    /**
     * Delete file or folder recursive.
     *
     * @param file file or folder to be deleted
     * @return true whether deletion succeeded, false otherwise
     */
    public static boolean deleteContentRecursive(java.io.File file)
    {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                boolean success = deleteRecursive(new java.io.File(file, child));
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }
}
