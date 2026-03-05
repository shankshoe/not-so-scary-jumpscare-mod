package com.rjs;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class RandomjumpscareClient implements ClientModInitializer {

    private KeyBinding triggerKey;
    private int tickCounter = 0;
    private static final int CHECK_INTERVAL = 20;
    private static final Random random = new Random();

    @Override
    public void onInitializeClient() {

        System.out.println("[RandomJumpscare] Client initialized.");

        // Register keybind (U triggers jumpscare)
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.randomjumpscare.trigger",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.randomjumpscare"
        ));

        // Process MP4s on startup (async)
        new Thread(Mp4ToJumpscare::processAllMp4s).start();

        // Listen for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (triggerKey.wasPressed()) {
                triggerRandomProcessedJumpscare();
            }
            tickCounter++;

            if (tickCounter >= CHECK_INTERVAL) {
                tickCounter = 0;

                int roll = random.nextInt(10000) + 1; // 1-10,000

                if (roll == 90) {
                    System.out.println("[RandomJumpscare] Random roll hit 90!");
                    triggerRandomProcessedJumpscare();
                }
            }
            
        });
    }

    private void triggerRandomProcessedJumpscare() {

        Path processedDir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("jumpassets")
                .resolve("processed");

        try {
            var folders = Files.list(processedDir)
        .filter(Files::isDirectory)
        .toList();
                    
        if (folders.isEmpty()) {
            System.out.println("[RandomJumpscare] No processed jumpscares found.");
            return;
        }

        int randomIndex = random.nextInt(folders.size());
        Path chosenFolder = folders.get(randomIndex);

        System.out.println("[RandomJumpscare] Triggering jumpscare: "
                + chosenFolder.getFileName());

        RandomJumpscareOverlay.play(chosenFolder);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}