package com.rjs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

public class RandomJumpscareOverlay {

    public static int idealticklimit = 10;
    public static boolean playing = false;
    private static List<Identifier> frameTextures = new ArrayList<>();
    public static int currentFrame = 0;
    public static int tickycounter = 0; // for framerate-independent timing

    private static HudRenderCallback renderCallback;

    public static boolean Retardedfix = false;
    public static int currentplayerfps;
    private static Clip currentClip = null;
    private static float jumpscareVolume = 1.0f;
    
    public static void play(Path processedFolder) {
        if (playing) return;

        Path framesDir = processedFolder.resolve("frames");
        Path audioFile = processedFolder.resolve("audio").resolve("audio.wav");

        if (!Files.exists(framesDir)) return;

        playing = true;
        frameTextures.clear();
        currentFrame = 0;
        tickycounter = 0;

        // Load frames asynchronously to prevent freezing
        new Thread(() -> {
            loadFrames(framesDir);

            // After frames are loaded, start audio and register renderer
            playAudio(audioFile);
            registerRenderer();
        }).start();
    }

    private static void loadFrames(Path framesDir) {
        try {
            Files.list(framesDir)
                    .sorted(Comparator.naturalOrder())
                    .forEach(path -> {
                        try (InputStream stream = Files.newInputStream(path)) {

                            // Load NativeImage from InputStream
                            net.minecraft.client.texture.NativeImage nativeImage =
                                    net.minecraft.client.texture.NativeImage.read(stream);

                            // Wrap in a texture
                            net.minecraft.client.texture.NativeImageBackedTexture texture =
                                    new net.minecraft.client.texture.NativeImageBackedTexture(nativeImage);

                            // Register dynamic texture and store Identifier
                            Identifier id = MinecraftClient.getInstance()
                                    .getTextureManager()
                                    .registerDynamicTexture(
                                            "jumpscare_" + path.getFileName().toString(),
                                            texture
                                    );

                            frameTextures.add(id);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void playAudio(Path audioFile) {
    if (!Files.exists(audioFile)) return;

    new Thread(() -> {
        try (AudioInputStream ais =
                     AudioSystem.getAudioInputStream(audioFile.toFile())) {

            // Stop old clip if one exists
            if (currentClip != null) {
                currentClip.stop();
                currentClip.close();
                currentClip = null;
            }

            currentClip = AudioSystem.getClip();
            currentClip.open(ais);

            applyVolume(); // Apply volume BEFORE playback

            currentClip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }).start();
}

    public static void setVolume(float volume) {
    jumpscareVolume = Math.max(0f, Math.min(1f, volume));
    applyVolume(); 
}
    public static float getVolume() {
        System.out.println("Current jumpscare volume: " + jumpscareVolume);
        return jumpscareVolume;
    }

    public static void applyVolume() {
    if (currentClip == null) return;

    if (!currentClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
        return; // Some systems don't support this
    }

    FloatControl gainControl =
            (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);

    if (jumpscareVolume <= 0f) {
        gainControl.setValue(gainControl.getMinimum());
        return;
    }

    float dB = (float) (20.0 * Math.log10(jumpscareVolume));

    // Clamp to supported range
    dB = Math.max(gainControl.getMinimum(),
         Math.min(gainControl.getMaximum(), dB));

    gainControl.setValue(dB);
}

    private static void registerRenderer() {
        renderCallback = (DrawContext context , RenderTickCounter tickCounter) -> {
            if (!playing || frameTextures.isEmpty()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            // Draw current frame fullscreen
            if (currentFrame < frameTextures.size()) {
                context.drawTexture(
                        frameTextures.get(currentFrame),
                        0,
                        0,
                        0,
                        0,
                        width,
                        height,
                        width,
                        height
                );
            }

            
            

                        
            
           
            
            tickycounter++;

            if (tickycounter >= getIdealTickLimit()) {
                tickycounter = 0;
                currentFrame++;               
            } 
            

            // Stop when finished
            if (currentFrame >= frameTextures.size()) {
                stop();
            }
        };
        if (!Retardedfix) {
            HudRenderCallback.EVENT.register(renderCallback);
            Retardedfix = true;
        }
    }
    
    public static int getdaframes(){
        return MinecraftClient.getInstance().getCurrentFps();
    }

    public static int getIdealTickLimit() {
        
        if(getdaframes() <= 100){
            idealticklimit = 5;
        } else {
            idealticklimit = Math.round(getdaframes() / 100.0f) * 100;
            idealticklimit = idealticklimit/10;
            System.out.println("Current FPS: " + getdaframes() + ", Ideal Tick Limit: " + idealticklimit);
            }
        return idealticklimit;
        
    }

    private static void stop() {
        playing = false;
        currentFrame = 0;
        tickycounter = 0;
        idealticklimit = 0;
        synchronized (frameTextures) {
        frameTextures.clear();
        }
        System.out.println("playing = " + playing);
        System.out.println("currentFrame = " + currentFrame);
        System.out.println("tickycounter = " + tickycounter);
    }
}