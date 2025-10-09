package rocamocha.mochamix.plugins;

import rocamocha.reactivemusic.ReactiveMusicDebug;
import rocamocha.reactivemusic.ReactiveMusicState;
import rocamocha.reactivemusic.api.ReactiveMusicPlugin;
import rocamocha.reactivemusic.api.ReactiveMusicAPI;
import rocamocha.reactivemusic.api.audio.ReactivePlayer;
import rocamocha.reactivemusic.api.audio.ReactivePlayerManager;
import rocamocha.reactivemusic.api.audio.ReactivePlayerOptions;
import rocamocha.reactivemusic.api.eventsys.EventRecord;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import rocamocha.mochamix.api.minecraft.MinecraftPlayer;
import rocamocha.mochamix.api.minecraft.MinecraftWorld;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plugin that detects player interactions with entities (right-click and left-click).
 * Plays audio files directly from songpack directories when interactions occur.
 */
public class EntityInteractionPlugin extends ReactiveMusicPlugin {
    public EntityInteractionPlugin() {
        super("mochamix", "entity_interaction");
    }
    
    // Audio player instances for different interaction types
    private static ReactivePlayer animalSoundPlayer;
    private static ReactivePlayer hostileSoundPlayer;
    private static ReactivePlayer cowSoundPlayer;
    private static ReactivePlayer chickenSoundPlayer;
    private static ReactivePlayer pigSoundPlayer;
    private static ReactivePlayer sheepSoundPlayer;
    
    // Audio player manager
    private static ReactivePlayerManager audioManager;
    
    @Override
    public void init() {
        // Get the audio manager
        audioManager = ReactiveMusicAPI.audioManager();
        
        // Create audio player instances for different entity types
        createAudioPlayers();
        
        // Register event handlers
        registerRightClickEvents();
        registerAttackEvents();
        
        ReactiveMusicDebug.LOGGER.info("EntityInteractionPlugin initialized - audio players created and listening for entity interactions");
    }
    
    private void createAudioPlayers() {
        try {
            // Create player options - these players are for short sound effects
            ReactivePlayerOptions options = ReactivePlayerOptions.create()
                .namespace("mochamix")
                .group("entity_interactions")
                .loop(false)
                .autostart(false);
            
            // Create individual players for each entity type
            animalSoundPlayer = audioManager.create("mochamix:animal_interaction", options);
            hostileSoundPlayer = audioManager.create("mochamix:hostile_interaction", options);
            cowSoundPlayer = audioManager.create("mochamix:cow_interaction", options);
            chickenSoundPlayer = audioManager.create("mochamix:chicken_interaction", options);
            pigSoundPlayer = audioManager.create("mochamix:pig_interaction", options);
            sheepSoundPlayer = audioManager.create("mochamix:sheep_interaction", options);
            
            ReactiveMusicDebug.LOGGER.info("Created {} audio players for entity interactions", 6);
            
        } catch (Exception e) {
            ReactiveMusicDebug.LOGGER.error("Failed to create audio players for EntityInteractionPlugin", e);
        }
    }
    
    private void registerRightClickEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Only handle main hand interactions to avoid duplicate events
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            
            String entityType = entity.getType().toString();
            String playerName = player.getName().getString();
            
            // Play audio and log interaction based on entity type
            if (entity instanceof CowEntity) {
                playEntitySound("cow", cowSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a cow", playerName);
            } else if (entity instanceof ChickenEntity) {
                playEntitySound("chicken", chickenSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a chicken", playerName);
            } else if (entity instanceof PigEntity) {
                playEntitySound("pig", pigSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a pig", playerName);
            } else if (entity instanceof SheepEntity) {
                playEntitySound("sheep", sheepSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a sheep", playerName);
            } else if (entity instanceof AnimalEntity) {
                playEntitySound("animal", animalSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked an animal: {}", playerName, entityType);
            } else if (entity instanceof HostileEntity) {
                playEntitySound("hostile", hostileSoundPlayer, "interact");
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a hostile mob: {}", playerName, entityType);
            } else if (entity instanceof MobEntity) {
                // Generic mob that's not hostile or animal
                ReactiveMusicDebug.LOGGER.info("Player {} right-clicked a mob: {}", playerName, entityType);
            }
            
            // Return PASS to allow other mods/vanilla to handle the event
            return ActionResult.PASS;
        });
    }
    
    private void registerAttackEvents() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            String entityType = entity.getType().toString();
            String playerName = player.getName().getString();
            
            // Play different sounds for attacks vs right-clicks
            if (entity instanceof AnimalEntity) {
                playEntitySound("animal", animalSoundPlayer, "attack");
                ReactiveMusicDebug.LOGGER.info("Player {} attacked an animal: {}", playerName, entityType);
            } else if (entity instanceof HostileEntity) {
                playEntitySound("hostile", hostileSoundPlayer, "attack");
                ReactiveMusicDebug.LOGGER.info("Player {} attacked a hostile mob: {}", playerName, entityType);
            }
            
            // Return PASS to allow other mods/vanilla to handle the event
            return ActionResult.PASS;
        });
    }
    
    /**
     * Plays a sound from the entity_interactions directory in the current songpack.
     * 
     * Expected songpack directory structure (with subdirectories and random file selection):
     * MySongpack/
     * ├── music/
     * │   └── (regular music files)
     * └── entity_interactions/
     *     ├── cow/
     *     │   ├── interact/
     *     │   │   ├── moo1.mp3
     *     │   │   ├── moo2.mp3
     *     │   │   └── moo3.mp3
     *     │   └── attack/
     *     │       ├── hurt1.mp3
     *     │       └── hurt2.mp3
     *     ├── chicken/
     *     │   └── interact/
     *     │       ├── cluck1.mp3
     *     │       └── cluck2.mp3
     *     ├── pig/
     *     ├── sheep/
     *     ├── animal/
     *     │   ├── interact/
     *     │   └── attack/
     *     └── hostile/
     *         ├── interact/
     *         └── attack/
     * 
     * @param soundType The type of sound (cow, chicken, animal, etc.)
     * @param audioPlayer The ReactivePlayer to use for playback
     */
    /**
     * Plays a random sound from a subdirectory within the entity_interactions directory.
     * 
     * @param soundType The type of sound (cow, chicken, animal, etc.) - used as subdirectory name
     * @param audioPlayer The ReactivePlayer to use for playback
     * @param subdirectory Optional subdirectory within the soundType directory (can be null)
     */
    private void playEntitySound(String soundType, ReactivePlayer audioPlayer, String subdirectory) {
        if (audioPlayer == null) {
            ReactiveMusicDebug.LOGGER.warn("Audio player is null for sound type: {}", soundType);
            return;
        }
        
        try {
            // Build the directory path
            String directoryPath = "entity_interactions/" + soundType;
            if (subdirectory != null && !subdirectory.isEmpty()) {
                directoryPath += "/" + subdirectory;
            }
            
            // Get a random audio file from the directory
            String randomFile = getRandomAudioFile(directoryPath);
            if (randomFile == null) {
                ReactiveMusicDebug.LOGGER.warn("No audio files found in directory: {}", directoryPath);
                return;
            }
            
            // Use setFile to play the selected file
            audioPlayer.setFile(randomFile);
            audioPlayer.play();
            
            ReactiveMusicDebug.LOGGER.debug("Playing entity interaction sound: {}", randomFile);
            
        } catch (Exception e) {
            ReactiveMusicDebug.LOGGER.error("Failed to play entity interaction sound for type: {}", soundType, e);
        }
    }
    
    /**
     * Gets a random audio file from the specified directory in the current songpack.
     * 
     * @param directoryPath The path relative to the songpack root (e.g., "entity_interactions/cow")
     * @return A random audio file path, or null if no files found
     */
    private String getRandomAudioFile(String directoryPath) {
        try {
            List<String> audioFiles = listAudioFilesInDirectory(directoryPath);
            if (audioFiles.isEmpty()) {
                return null;
            }
            
            // Select a random file
            Random random = new Random();
            String selectedFile = audioFiles.get(random.nextInt(audioFiles.size()));
            
            ReactiveMusicDebug.LOGGER.debug("Selected random file: {} from {} options in {}", selectedFile, audioFiles.size(), directoryPath);
            return selectedFile;
            
        } catch (Exception e) {
            ReactiveMusicDebug.LOGGER.error("Failed to get random audio file from directory: {}", directoryPath, e);
            return null;
        }
    }
    
    /**
     * Lists all audio files in a directory within the current songpack.
     * 
     * @param directoryPath The path relative to the songpack root
     * @return List of audio file paths (relative to songpack root)
     */
    private List<String> listAudioFilesInDirectory(String directoryPath) {
        List<String> audioFiles = new ArrayList<>();
        
        try {
            // Get current songpack info
            if (ReactiveMusicState.currentSongpack == null) {
                ReactiveMusicDebug.LOGGER.warn("No current songpack loaded");
                return audioFiles;
            }
            
            Path songpackPath = ReactiveMusicState.currentSongpack.getPath();
            boolean isEmbedded = ReactiveMusicState.currentSongpack.isEmbedded();
            
            if (isEmbedded) {
                // For embedded songpacks, we can't easily list files
                // Fall back to trying common audio file names
                ReactiveMusicDebug.LOGGER.debug("Embedded songpack detected, using fallback file names for: {}", directoryPath);
                return getFallbackAudioFiles(directoryPath);
            }
            
            Path targetDir;
            
            // Handle zip files
            if (Files.isRegularFile(songpackPath) && songpackPath.toString().endsWith(".zip")) {
                try (FileSystem fs = FileSystems.newFileSystem(songpackPath, (ClassLoader) null)) {
                    targetDir = fs.getPath("/" + directoryPath);
                    if (Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                        audioFiles = Files.list(targetDir)
                            .filter(Files::isRegularFile)
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .filter(this::isAudioFile)
                            .map(fileName -> directoryPath + "/" + fileName)
                            .collect(Collectors.toList());
                    }
                }
            } 
            // Handle regular directories
            else if (Files.isDirectory(songpackPath)) {
                targetDir = songpackPath.resolve(directoryPath);
                if (Files.exists(targetDir) && Files.isDirectory(targetDir)) {
                    audioFiles = Files.list(targetDir)
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(this::isAudioFile)
                        .map(fileName -> directoryPath + "/" + fileName)
                        .collect(Collectors.toList());
                }
            }
            
        } catch (IOException e) {
            ReactiveMusicDebug.LOGGER.error("Failed to list files in directory: {}", directoryPath, e);
        }
        
        return audioFiles;
    }
    
    /**
     * Checks if a file is an audio file based on its extension.
     */
    private boolean isAudioFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".mp3") || 
               lowerName.endsWith(".wav") || 
               lowerName.endsWith(".ogg") || 
               lowerName.endsWith(".flac");
    }
    
    /**
     * Provides fallback file names for embedded songpacks where we can't list directories.
     */
    private List<String> getFallbackAudioFiles(String directoryPath) {
        List<String> fallbackFiles = new ArrayList<>();
        
        // Try common numbered variations
        for (int i = 1; i <= 5; i++) {
            String fileName = directoryPath + "/sound" + i + ".mp3";
            fallbackFiles.add(fileName);
        }
        
        // Add the base sound file
        fallbackFiles.add(directoryPath + "/sound.mp3");
        
        return fallbackFiles;
    }
    
    @Override
    public void gameTick(MinecraftPlayer player, MinecraftWorld world, Map<EventRecord, Boolean> eventMap) {
        // This plugin doesn't use the songpack event system, 
        // it plays audio directly through ReactivePlayer instances
        // No game tick logic needed for this implementation
    }
}