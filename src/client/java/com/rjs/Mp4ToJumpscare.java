package com.rjs;


import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

public class Mp4ToJumpscare {

    private static final int FPS = 20;

    public static void processAllMp4s() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path jumpAssets = configDir.resolve("jumpassets");
        Path rawDir = jumpAssets.resolve("raw");
        Path processedDir = jumpAssets.resolve("processed");

        try {
            Files.createDirectories(rawDir);
            Files.createDirectories(processedDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        File[] mp4Files = rawDir.toFile().listFiles((dir, name) ->
                name.toLowerCase().endsWith(".mp4"));

        if (mp4Files == null || mp4Files.length == 0) {
            System.out.println("[RandomJumpscare] No MP4 files found.");
            return;
        }

        for (File mp4 : mp4Files) {
            String name = mp4.getName().replace(".mp4", "");
            Path outputFolder = processedDir.resolve(name);
            Path framesDir = outputFolder.resolve("frames");
            Path audioFile = outputFolder.resolve("audio").resolve("audio.wav");

            if (isAlreadyProcessed(framesDir, audioFile)) {
                System.out.println("[RandomJumpscare] Skipping already processed: " + name);
                continue;
            }

            // Clean incomplete folder if it exists
            deleteDirectory(outputFolder);

            try {
                Files.createDirectories(framesDir);
                Files.createDirectories(audioFile.getParent());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            runFfmpeg(mp4, framesDir, audioFile);
        }
    }

    private static boolean isAlreadyProcessed(Path framesDir, Path audioFile) {
        if (!Files.exists(framesDir)) return false;
        if (!Files.exists(audioFile)) return false;

        try {
            return Files.list(framesDir).findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static void runFfmpeg(File input, Path framesDir, Path audioFile) {
        String frameOutput = framesDir.resolve("frame_%04d.png").toString();

        ProcessBuilder frameProcess = new ProcessBuilder(
                "ffmpeg",
                "-i", input.getAbsolutePath(),
                "-vf", "fps=" + FPS,
                frameOutput
        );

        ProcessBuilder audioProcess = new ProcessBuilder(
                "ffmpeg",
                "-i", input.getAbsolutePath(),
                "-vn",
                "-acodec", "pcm_s16le",
                audioFile.toString()
        );

        try {
            frameProcess.inheritIO().start().waitFor();
            audioProcess.inheritIO().start().waitFor();
            System.out.println("[RandomJumpscare] Processed: " + input.getName());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(Path path) {
        if (!Files.exists(path)) return;

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}