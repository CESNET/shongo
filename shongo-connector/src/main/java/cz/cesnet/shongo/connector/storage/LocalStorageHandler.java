package cz.cesnet.shongo.connector.storage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link LocalStorageHandler} can manage file and folders in local directory.
 * This test will fail on Windows
 * TODO: solve absolute path on multi OS level
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LocalStorageHandler
{
    private static Logger logger = LoggerFactory.getLogger(LocalStorageHandler.class);

    /**
     * Maximum number of consequent resumings.
     */
    private final int MAX_RESUME_COUNT = 5;

    /**
     * Duration in milliseconds to sleep before resuming.
     */
    private final int WAIT_SLEEP = 100;

    /**
     * URL of the {@link LocalStorageHandler}.
     */
    private String url;

    /**
     * List of folders and delete (request from another Thread)
     */
    private List<String> foldersBeingDeleted = new CopyOnWriteArrayList<String>();

    /**
     * Map of files to be created or that are being copied now (entry key represents a fileId and entry value
     * represents a recordingFolderId of folder for the file to be created in)
     */
    private ConcurrentHashMap<String, String> filesBeingCreated = new ConcurrentHashMap<String, String>();

    /**
     * Constructor.
     *
     * @param url sets the {@link #url}
     */
    public LocalStorageHandler(String url) throws FileNotFoundException {
        url = new java.io.File(url).getPath();
        if (url.endsWith("/") || url.endsWith("\\")) {
            url = url.substring(0,url.length() - 1);
        }
        this.url = url;
        if (!rootFolderExists()) {
            throw new FileNotFoundException("Storage directory '" + url  + "' doesn't exist.");
        }
    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    public boolean rootFolderExists()
    {
        return getFileInstance(url).exists();
    }

    /**
     * Create a new folder.
     *
     * @param folder information about the new folder, if parentFolderId is {@coe null}, folder is created in /
     * @return id of the new folder
     */
    public String createFolder(Folder folder) throws FileNotFoundException {
        folder.setFolderName(mangle(folder.getFolderName()));
        String folderId = getChildId(folder.getParentFolderId(), folder.getFolderName());
        String folderUrl = getUrlFromId(folderId);

        if (!rootFolderExists()) {
            throw new FileNotFoundException("Directory '" + folderUrl + "' can't be created, , because the parent folder is not accessible.");
        }

        java.io.File file = getFileInstance(folderUrl);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new FileNotFoundException("Directory '" + file.getAbsolutePath() + "' can't be created.");
            }
        }
        return folderId;
    }

    /**
     * Mangle filenames - simple version based on shongo naming convention
     * TODO: general solution
     * @param filename
     * @return
     */
    private static String mangle(String filename) {
        return filename.replace(":","_");
    }

    /**
     * Delete existing folder.
     *
     * @param folderId id of existing folder which should be deleted
     */
    public void deleteFolder(String folderId)
    {
        try {
            foldersBeingDeleted.add(folderId);
            while (filesBeingCreated.containsValue(folderId)) {
                try {
                    Thread.sleep(WAIT_SLEEP);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
            }

            String folderUrl = getUrlFromId(folderId);
            java.io.File ioFolder = getFileInstance(folderUrl);
            if (rootFolderExists()) {
                if (ioFolder.exists() && !deleteRecursive(ioFolder)) {
                    throw new RuntimeException("Directory '" + folderUrl + "' cannot be deleted.");
                }
            } else {
                //TODO: vyresit budouci mazani
                throw new RuntimeException("Folder \"" + folderId + "\" cannot be deleted, because file system is not accessible.");
            }
        } finally {
            foldersBeingDeleted.remove(folderId);
        }
    }

    /**
     * Test if folder already exists.
     *
     * @param folderId folder to be tested
     * @return {@value true} if folder exists, {@value false} otherwise
     */
    public boolean folderExists(String folderId)
    {
        String folderUrl = getUrlFromId(folderId);
        return getFileInstance(folderUrl).exists();
    }

    /**
     * List folders in specified parent folder or root folders.
     *
     * @param folderId   id of the parent folder which contains requested folders which or {@code null}
     * @param folderName substring which must be contained in {@link Folder#folderName} or {@code null}
     * @return list of {@link Folder}s
     */
    public List<Folder> listFolders(String folderId, final String folderName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioParentFolder = getFileInstance(folderUrl);
        String[] ioFolders = ioParentFolder.list(new FilenameFilter() {
            @Override
            public boolean accept(java.io.File directory, String fileName) {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isDirectory() || fileName.equals(".") || fileName.equals("..")) {
                    return false;
                }
                return folderName == null || StringUtils.containsIgnoreCase(fileName, folderName);
            }
        });
        List<Folder> folders = new LinkedList<Folder>();
        if (ioFolders != null) {
            for (String ioFolder : ioFolders) {
                Folder folder = new Folder();
                folder.setFolderId(getChildId(folderId, ioFolder));
                folder.setParentFolderId(folderId);
                folder.setFolderName(ioFolder);
                folders.add(folder);
            }
        }
        return folders;
    }

    /**
     * Create a new file in existing folder.
     *
     * @param file          information about the new file
     * @param fileContent   input stream from which the file content can be read
     * @param resumeSupport to be used for re-opening given {@code fileContent}
     */
    public void createFile(File file, InputStream fileContent, ResumeSupport resumeSupport)
    {
        String folderId = file.getFolderId();
        String folderUrl = getUrlFromId(folderId);
        file.setFileName(mangle(file.getFileName()));
        String fileName = file.getFileName();
//        String fileUrl = getChildPath(folderUrl, fileName);
        java.io.File ioFile = getFileInstance(file);
        String fileUrl = ioFile.getAbsolutePath();

        if (ioFile.exists()) {
            throw new RuntimeException("File '" + fileUrl + "' already exists.");
        }

        if (folderId != null) {
            filesBeingCreated.put(fileName, folderUrl);
        }
        try {
            int fileContentIndex = 0;
            OutputStream fileOutputStream = new FileOutputStream(fileUrl);
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    // Read next bytes into buffer
                    int readResumeCount = MAX_RESUME_COUNT;
                    int bytesRead;
                    while (true) {
                        try {
                            bytesRead = fileContent.read(buffer, 0, buffer.length);
                            readResumeCount = MAX_RESUME_COUNT;
                            break;
                        }
                        catch (IOException exception) {
                            // Check if resume isn't available
                            if (resumeSupport == null || --readResumeCount <= 0) {
                                throw exception;
                            }

                            String message = "Creation of file " + getChildPath(folderUrl, fileName) + ": ";
                            logger.warn(message + "Reading data failed at " + fileContentIndex + ".", exception);

                            // Wait before resuming
                            try {
                                Thread.sleep(WAIT_SLEEP);
                            }
                            catch (InterruptedException sleepException) {
                                Thread.currentThread().interrupt();
                                logger.warn("Thread.sleep", sleepException);
                            }

                            // Reopen file content stream
                            logger.info(message + "Trying to resume the reading the data at {}...", fileContentIndex);
                            InputStream oldFileContent = fileContent;
                            try {
                                fileContent = resumeSupport.reopenInputStream(fileContent, fileContentIndex);
                                logger.info(message + "Resume succeeded, continuing in file creation...");
                            }
                            catch (Exception resumeException) {
                                throw new RuntimeException("Reopening input stream failed for creation of file " +
                                        getChildPath(folderUrl, fileName) + ".", resumeException);
                            }
                            finally {
                                try {
                                    oldFileContent.close();
                                }
                                catch (IOException closeException) {
                                    logger.debug("Failed to close old input stream...");
                                }
                            }
                        }
                        catch (Exception exception) {
                            throw new RuntimeException("Reading input stream failed for creation of file " +
                                    getChildPath(folderUrl, fileName) + " at " + fileContentIndex + ".", exception);
                        }
                    }

                    // Check for end of file content
                    if (bytesRead == -1) {
                        break;
                    }
                    // Check if folder isn't already deleted
                    if (foldersBeingDeleted.contains(file.getFolderId())) {
                        logger.warn("Creation of file " + getChildPath(folderUrl, fileName) +
                                " has been stopped because folder " + folderId + " is being deleted.");
                        break;
                    }
                    // Write bytes from buffer
                    int writeResumeCount = MAX_RESUME_COUNT;
                    while (true) {
                        try {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            writeResumeCount = MAX_RESUME_COUNT;
                            break;
                        }
                        catch (IOException exception) {
                            // Close output stream
                            fileOutputStream.close();

                            // Check if resume isn't available
                            if (--writeResumeCount <= 0) {
                                throw exception;
                            }

                            // Resume writing
                            try {
                                fileOutputStream = new FileOutputStream(fileUrl);
                            }
                            catch (Exception resumeException) {
                                throw new RuntimeException("Reopening output stream failed for creation of file " +
                                        file.getFolderId() + ":" + file.getFileName() + ".", resumeException);
                            }
                            logger.warn("Writing file {}:{} failed at {}, resuming...", new Object[]{
                                    file.getFolderId(), file.getFileName(), fileContentIndex
                            });
                            try {
                                Thread.sleep(WAIT_SLEEP);
                            }
                            catch (InterruptedException sleepException) {
                                Thread.currentThread().interrupt();
                                logger.warn("Thread.sleep", sleepException);
                            }
                        }
                    }

                    // Move file content
                    fileContentIndex += bytesRead;
                }
            }
            finally {
                fileContent.close();
                fileOutputStream.close();
            }
        }
        catch (IOException exception) {
            throw new RuntimeException("File '" + fileUrl + "' cannot be created.", exception);
        }
        finally {
            filesBeingCreated.remove(fileName,folderUrl);
        }
    }

    /**
     * Create a new file in existing folder in the storage.
     *
     * @param file        information about the new file
     * @param fileContent input stream from which the file content can be read
     */
    public final void createFile(File file, InputStream fileContent)
    {
        createFile(file, fileContent, null);
    }

    /**
     * Delete existing file.
     *
     * @param folderId
     * @param fileName
     */
    public void deleteFile(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        String fileUrl = getChildPath(folderUrl, mangle(fileName));
        java.io.File file = getFileInstance(fileUrl);
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("File'" + fileUrl + "' cannot be deleted.");
            }
        }
    }

    /**
     * Test if file already exists.
     *
     * @param file file to be tested
     * @return {@value true} if file exists, {@value false} otherwise
     */
    public boolean fileExists(File file)
    {
        String folderUrl = getUrlFromId(file.getFolderId());
        String fileUrl = getChildPath(folderUrl, mangle(file.getFileName()));
        return getFileInstance(fileUrl).exists();
    }

    /**
     * List files in specified .
     *
     * @param folderId id of the folder which contains requested files
     * @param fileName substring which must be contained in {@link File#fileName} or {@code null}
     * @return list of {@link File}s
     */
    public List<File> listFiles(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioFolder = getFileInstance(folderUrl);
        if (!ioFolder.exists()) {
            throw new RuntimeException("Folder '" + folderUrl + "' doesn't exist.");
        }
        String[] ioFiles = ioFolder.list(new FilenameFilter()
        {
            @Override
            public boolean accept(java.io.File directory, String fileName)
            {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isFile()) {
                    return false;
                }
                return fileName == null || StringUtils.containsIgnoreCase(file.getName(), fileName);
            }
        });
        List<File> files = new LinkedList<File>();
        if (ioFiles != null) {
            for (String ioFile : ioFiles) {
                File file = new File();
                file.setFolderId(folderId);
                file.setFileName(ioFile);
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Download content of existing file.
     *
     * @param folderId id of the folder in which the file is located
     * @param fileName id of the file in the folder
     * @return {@link InputStream} with the file content
     */
    public InputStream getFileContent(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        String fileUrl = getChildPath(folderUrl, mangle(fileName));

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileUrl);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("File " + fileUrl + " doesn't exist.");
        }

        return inputStream;
    }

    /**
     * @param id
     * @return url from folderId or fileId
     */
    public String getUrlFromId(String id)
    {
        if (id == null) {
            return getUrl();
        }
        else {
            return getChildPath(getUrl(), id);
        }
    }

    /**
     * @param parentId
     * @param childName
     * @return childId from {@code parentId} and {@code childName}
     */
    public static String getChildId(String parentId, String childName)
    {
        if (parentId == null) {
            return childName;
        }
        else {
            java.io.File parent = new java.io.File("parentId");
            return new java.io.File(parent, childName).getPath();
        }
    }

    /**
     * @param parentUrl
     * @param childName
     * @return childUrl from {@code parentUrl} and {@code childName}
     */
    public static String getChildPath(String parentUrl, String childName)
    {
        java.io.File path = new java.io.File(parentUrl);
        return new java.io.File(path, mangle(childName)).getPath();

//        StringBuilder childUrl = new StringBuilder();
//        childUrl.append(parentUrl);
//        if (childUrl.charAt(childUrl.length() - 1) != '/') {
//            childUrl.append("/");
//        }
//        childUrl.append(mangle(childName));
//        return childUrl.toString();
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
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteRecursive(new java.io.File(file, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

//    private URI constructURI(String url)
//    {
////        String stringUri = "file://" + url;
////        try {
////            return new URI(stringUri);
//            return new java.io.File(url).toURI();
////        } catch (URISyntaxException e) {
////            throw new RuntimeException("Syntax error in directory path'" + url + "'.");
////        }
//    }

    /**
     * Returns new instance of #link(java.io.File) for given url.
     *
     * @param url
     * @return
     */
    private java.io.File getFileInstance(String url)
    {
//        URI uri = constructURI(url);
//        return new java.io.File(uri.getPath());
        return new java.io.File(url);
    }

    /**
     * Returns new instance of #link(java.io.File) for given url.
     *
     * @param file
     * @return
     */
    private java.io.File getFileInstance(File file)
    {
        String folderId = file.getFolderId();
        String folderUrl = getUrlFromId(folderId);
        file.setFileName(mangle(file.getFileName()));
        String fileName = file.getFileName();
        String fileUrl = getChildPath(folderUrl, fileName);

        return new java.io.File(fileUrl);
    }

    /**
     * Returns true if expected size differs from file size at most 10 MB
     * @param file
     * @param expectedSize
     * @return
     */
    public boolean validateFile(File file, long expectedSize)
    {
        long fileSize = getFileInstance(file).length();
        return Math.abs(fileSize - expectedSize) < (10 * 1024 * 1024);
    }

    public boolean filenameEqualsFileId(File file, String fileId)
    {
        return file.getFileName().contains(mangle(fileId));
    }
}
