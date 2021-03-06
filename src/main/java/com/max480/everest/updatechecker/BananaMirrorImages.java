package com.max480.everest.updatechecker;

import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BananaMirrorImages {
    private static final Logger log = LoggerFactory.getLogger(BananaMirrorImages.class);

    public static void main(String[] args) throws IOException {
        if (Main.serverConfig.bananaMirrorConfig == null) {
            // if the info wasn't filled out, turn off mirror updating.
            return;
        }

        // load the list of existing mods.
        List<Map<String, Object>> modSearchDatabase;
        try (InputStream stream = new FileInputStream("uploads/modsearchdatabase.yaml")) {
            modSearchDatabase = new Yaml().load(stream);
        }

        // load the list of files that are already in the mirror.
        List<String> bananaMirrorList = listFiles();
        Set<String> toDelete = new HashSet<>(bananaMirrorList);

        for (Map<String, Object> mod : modSearchDatabase) {
            List<String> screenshots = (List<String>) mod.get("Screenshots");

            // we want to only mirror the 2 first screenshots.
            for (int i = 0; i < screenshots.size() && i < 2; i++) {
                String screenshotUrl = screenshots.get(i);
                String screenshotId = screenshotUrl.substring("https://images.gamebanana.com/".length(), screenshotUrl.lastIndexOf(".")).replace("/", "_") + ".png";

                if (bananaMirrorList.contains(screenshotId)) {
                    // existing file! this file should be kept.
                    toDelete.remove(screenshotId);
                } else {
                    // file is new!
                    downloadFile(screenshotUrl, screenshotId, bananaMirrorList);
                }
            }
        }

        // delete all files that disappeared from the database.
        for (String file : toDelete) {
            deleteFile(file, bananaMirrorList);
        }
    }

    private static void downloadFile(String screenshotUrl, String screenshotId, List<String> fileList) throws IOException {
        // download the screenshot
        DatabaseUpdater.runWithRetry(() -> {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream("image_to_read"))) {
                IOUtils.copy(new BufferedInputStream(DatabaseUpdater.openStreamWithTimeout(new URL(screenshotUrl))), os);
                return null; // to fullfill this stupid method signature
            }
        });

        // minimize it to 220px
        Thumbnails.of(new File("image_to_read"))
                .size(220, 220)
                .outputFormat("png")
                .toFile("thumb.png");

        // upload to Banana Mirror
        uploadFile(Paths.get("thumb.png"), screenshotId, fileList);
        FileUtils.forceDelete(new File("image_to_read"));
        FileUtils.forceDelete(new File("thumb.png"));
    }

    private static List<String> listFiles() throws IOException {
        try (FileInputStream is = new FileInputStream("banana_mirror_images.yaml")) {
            return new Yaml().load(is);
        }
    }

    private static void uploadFile(Path filePath, String fileId, List<String> fileList) throws IOException {
        // actually upload the file
        BananaMirror.makeSftpAction(Main.serverConfig.bananaMirrorConfig.imagesDirectory,
                channel -> channel.put(filePath.toAbsolutePath().toString(), fileId));

        // add the file to the list of files that are actually on the mirror, and write it to disk.
        fileList.add(fileId);
        try (FileOutputStream os = new FileOutputStream("banana_mirror_images.yaml")) {
            IOUtils.write(new Yaml().dump(fileList), os, "UTF-8");
        }

        log.info("Uploaded {} to Banana Mirror", fileId);
    }

    private static void deleteFile(String fileId, List<String> fileList) throws IOException {
        BananaMirror.makeSftpAction(Main.serverConfig.bananaMirrorConfig.imagesDirectory, channel -> channel.rm(fileId));

        // delete the file from the list of files that are actually on the mirror, and write it to disk.
        fileList.remove(fileId);
        try (FileOutputStream os = new FileOutputStream("banana_mirror_images.yaml")) {
            IOUtils.write(new Yaml().dump(fileList), os, "UTF-8");
        }

        log.info("Deleted {} from Banana Mirror", fileId);
    }
}
