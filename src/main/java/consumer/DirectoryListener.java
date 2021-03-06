package consumer;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Created by nicob on 02.11.2016.
 */

public class DirectoryListener implements Runnable {

    private static DirectoryListener instance;
    private static String filePath;

    private DirectoryListener(String filePath) {
        this.filePath = filePath;
    }

    public static DirectoryListener getDirectoryListener(String filePath){
        if (instance == null){
            instance = new DirectoryListener(filePath);
        }
        return instance;
    }

    private static void checkDirectory(Path path) {
        try {
            boolean isFolder = (boolean) Files.getAttribute(path, "basic:isDirectory", LinkOption.NOFOLLOW_LINKS);

            if (!isFolder){
                throw new IllegalArgumentException("Path: " + path + " is not a folder!");
            }
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }

        System.out.println("Watching path: " + path);

        FileSystem fileSystem = path.getFileSystem();

        try (WatchService watchService = fileSystem.newWatchService()){
            path.register(watchService, ENTRY_CREATE); //wenn andere Events (ENTRY_MODIFY/ENTRY_DELETE gewuenscht sind, diese per Komma anhaengen

            WatchKey watchKey = null;

            while (true) {
                watchKey = watchService.take();

                WatchEvent.Kind kind = null;
                for (WatchEvent watchEvent : watchKey.pollEvents()) {
                    kind = watchEvent.kind();
                    if (OVERFLOW == kind) {
                        continue;
                    }
                    else if (ENTRY_CREATE == kind){
                        Path filePath = ((WatchEvent<Path>) watchEvent).context();
                        String file = path + fileSystem.getSeparator() + filePath.toString();
                        BufferedReader data = new BufferedReader(new FileReader(file));
                        String jsonString = data.readLine();
                        while (jsonString != null){
                            System.out.println(jsonString);
                            jsonString = data.readLine();
                        }
                        data.close();
                    }
                }

                if (!watchKey.reset()){
                    break;
                }
            }
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (InterruptedException inEx) {
            inEx.printStackTrace();
        }
    }

    @Override
    public void run() {
        File directory = new File(filePath);
        checkDirectory(directory.toPath());
    }
}