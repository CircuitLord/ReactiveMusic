package circuitlord.reactivemusic;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.gui.YACLScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SymlinkWarningScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourcePackOpener;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.path.SymlinkEntry;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class RMScreen extends YACLScreen {


    public RMScreen(YetAnotherConfigLib config, Screen parent) {
        super(config, parent);
    }

    public void filesDragged(List<Path> paths) {
        String fileNames = (String)streamFileNames(paths).collect(Collectors.joining(", "));
        this.client.setScreen(new ConfirmScreen((confirmed) -> {
            if (confirmed) {
                List<Path> validPaths = new ArrayList(paths.size());
                Set<Path> set = new HashSet(paths);
                ResourcePackOpener<Path> resourcePackOpener = new ResourcePackOpener<Path>(this.client.getSymlinkFinder()) {
                    protected Path openZip(Path path) {
                        return path;
                    }

                    protected Path openDirectory(Path path) {
                        return path;
                    }
                };
                List<SymlinkEntry> list3 = new ArrayList();
                Iterator pathsIterator = paths.iterator();

                while(pathsIterator.hasNext()) {
                    Path path = (Path)pathsIterator.next();

                    try {
                        Path path2 = (Path)resourcePackOpener.open(path, list3);
                        if (path2 == null) {
                            ReactiveMusic.LOGGER.warn("Path {} does not seem like pack", path);
                        } else {
                            validPaths.add(path2);
                            set.remove(path2);
                        }
                    } catch (IOException var10) {
                        IOException iOException = var10;
                        ReactiveMusic.LOGGER.warn("Failed to check {} for packs", path, iOException);
                    }
                }

                if (!list3.isEmpty()) {
                    this.client.setScreen(SymlinkWarningScreen.pack(() -> {
                        this.client.setScreen(this);
                    }));
                    return;
                }

                if (!validPaths.isEmpty()) {
                    copyPacks(this.client, validPaths);
                    //this.refresh();
                }

                if (!set.isEmpty()) {
                    String string = (String)streamFileNames(set).collect(Collectors.joining(", "));
                    this.client.setScreen(new NoticeScreen(() -> {
                        this.client.setScreen(this);
                    }, Text.translatable("pack.dropRejected.title"), Text.translatable("pack.dropRejected.message", new Object[]{string})));
                    return;
                }
            }

            this.client.setScreen(this);
        }, Text.translatable("pack.dropConfirm"), Text.literal("Confirm Drop")));
    }

    private static Stream<String> streamFileNames(Collection<Path> paths) {
        return paths.stream().map(Path::getFileName).map(Path::toString);
    }

    protected static void copyPacks(MinecraftClient client, List<Path> srcPaths) {

        var gamePath = FabricLoader.getInstance().getGameDir();
        Path destPath = gamePath.resolve("resourcepacks");


        MutableBoolean mutableBoolean = new MutableBoolean();
        srcPaths.forEach((src) -> {
            try {
                Stream<Path> stream = Files.walk(src);

                try {
                    stream.forEach((toCopy) -> {
                        try {
                            Util.relativeCopy(src.getParent(), destPath, toCopy);
                        } catch (IOException var5) {
                            IOException iOException = var5;
                            ReactiveMusic.LOGGER.warn("Failed to copy datapack file  from {} to {}", new Object[]{toCopy, destPath, iOException});
                            mutableBoolean.setTrue();
                        }

                    });
                } catch (Throwable var7) {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                if (stream != null) {
                    stream.close();
                }
            } catch (IOException var8) {
                ReactiveMusic.LOGGER.warn("Failed to copy datapack file from {} to {}", src, destPath);
                mutableBoolean.setTrue();
            }

        });
        if (mutableBoolean.isTrue()) {
            SystemToast.addPackCopyFailure(client, destPath.toString());
        }

    }

}