package circuitlord.reactivemusic.config;

import circuitlord.reactivemusic.RMSongpackLoader;
import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.SongpackZip;
import circuitlord.reactivemusic.platform.PlatformHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
//? if >=1.20 {
/*import net.minecraft.client.gui.DrawContext;
*///?}
//? if >=1.21.9 {
/*import net.minecraft.client.gui.Click;
*///?}
//? if <1.20 {
import net.minecraft.client.util.math.MatrixStack;
//?}
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ModConfig {

    public static final ConfigStore GSON = new ConfigStore();

    public static ModConfig getConfig() {
        return GSON.instance();
    }

    public static void saveConfig() {
        GSON.save();
    }

    public MusicDelayLength musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;

    public MusicSwitchSpeed musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;

    public boolean debugModeEnabled = false;

    public String loadedUserSongpack = "";

    public List<String> blacklistedDimensions = new ArrayList<>();

    public HashMap<String, Vec3d> savedHomePositions = new HashMap<>();

    public List<String> soundsMuteMusic = new ArrayList<>();

    public List<String> soundsMuteMusicIgnoreDistance = new ArrayList<>();

    public boolean hasForcedInitialVolume = false;

    public static Screen createScreen(Screen parent) {
        return new VanillaConfigScreen(parent, 0, -1);
    }

    public static void setActiveSongpack(SongpackZip songpack) {
        if (songpack.embedded) {
            getConfig().loadedUserSongpack = "";
        } else {
            getConfig().loadedUserSongpack = songpack.config.name;
        }

        GSON.save();
        ReactiveMusic.setActiveSongpack(songpack);
    }

    private static class VanillaConfigScreen extends Screen {
        private static final int ROW_HEIGHT = 44;
        private static final int SONGPACK_START_Y = 164;
        private static final int BUTTON_HEIGHT = 20;
        private static final int LABEL_COLOR = 0xFFFFFF;
        private static final int MUTED_COLOR = 0xA0A0A0;
        private static final int ERROR_COLOR = 0xFF7777;
        private static final int WARNING_COLOR = 0xFFD35A;
        private static final int SELECTED_BG = 0x553C6EA8;
        private static final int PANEL_BG = 0x66000000;
        private static final int PANEL_BORDER = 0x88FFFFFF;

        private final Screen parent;
        private boolean suppressBackgroundRender;
        private int page;
        private int selectedIndex;
        private int detailsScroll;

        private VanillaConfigScreen(Screen parent, int page, int selectedIndex) {
            super(Text.literal("Reactive Music"));
            this.parent = parent;
            this.page = page;
            RMSongpackLoader.fetchAvailableSongpacks();
            this.selectedIndex = selectedIndex >= 0 ? selectedIndex : defaultSelectedIndex();
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int controlX = Math.min(centerX + 18, this.width - 170);
            int controlWidth = Math.min(150, Math.max(100, this.width - controlX - 20));
            ModConfig config = getConfig();

            addDrawableChild(button(controlX, 42, controlWidth, BUTTON_HEIGHT, enumText(config.musicDelayLength2), b -> {
                config.musicDelayLength2 = next(config.musicDelayLength2, MusicDelayLength.values());
                saveConfig();
                b.setMessage(enumText(config.musicDelayLength2));
            }));

            addDrawableChild(button(controlX, 68, controlWidth, BUTTON_HEIGHT, enumText(config.musicSwitchSpeed2), b -> {
                config.musicSwitchSpeed2 = next(config.musicSwitchSpeed2, MusicSwitchSpeed.values());
                saveConfig();
                b.setMessage(enumText(config.musicSwitchSpeed2));
            }));

            addDrawableChild(button(controlX, 94, controlWidth, BUTTON_HEIGHT, toggleText(config.debugModeEnabled), b -> {
                config.debugModeEnabled = !config.debugModeEnabled;
                saveConfig();
                if (this.client != null) {
                    this.client.setScreen(new VanillaConfigScreen(parent, page, selectedIndex));
                }
            }));

            addDrawableChild(button(centerX - 80, 120, 160, BUTTON_HEIGHT, Text.literal("Open Songpack Folder"), b -> openSongpackFolder()));

            int perPage = rowsPerPage();
            int pages = totalPages(perPage);
            page = Math.max(0, Math.min(page, pages - 1));
            selectedIndex = clampSelectedIndex(selectedIndex);
            int start = page * perPage;
            int end = Math.min(RMSongpackLoader.availableSongpacks.size(), start + perPage);

            int loadButtonX = listRight() - 78;
            for (int i = start; i < end; i++) {
                SongpackZip songpack = RMSongpackLoader.availableSongpacks.get(i);
                int y = SONGPACK_START_Y + (i - start) * ROW_HEIGHT + 2;
                boolean failed = songpack.blockLoading;
                boolean loaded = isLoaded(songpack);
                int index = i;
                ButtonWidget loadButton = button(loadButtonX, y + 5, 76, BUTTON_HEIGHT, Text.literal(failed ? "Failed" : loaded ? "Loaded" : "Load"), b -> {
                    selectedIndex = index;
                    detailsScroll = 0;
                    setActiveSongpack(songpack);
                    if (this.client != null) {
                        this.client.setScreen(new VanillaConfigScreen(parent, page, selectedIndex));
                    }
                });
                loadButton.active = !failed && !loaded;
                addDrawableChild(loadButton);
            }

            ButtonWidget prev = button(centerX - 154, this.height - 28, 70, BUTTON_HEIGHT, Text.literal("Previous"), b -> {
                if (this.client != null) {
                    this.client.setScreen(new VanillaConfigScreen(parent, page - 1, selectedIndex));
                }
            });
            prev.active = page > 0;
            addDrawableChild(prev);

            addDrawableChild(button(centerX - 40, this.height - 28, 80, BUTTON_HEIGHT, Text.literal("Done"), b -> close()));

            ButtonWidget next = button(centerX + 84, this.height - 28, 70, BUTTON_HEIGHT, Text.literal("Next"), b -> {
                if (this.client != null) {
                    this.client.setScreen(new VanillaConfigScreen(parent, page + 1, selectedIndex));
                }
            });
            next.active = page + 1 < pages;
            addDrawableChild(next);
        }

        //? if >=1.21.9 {
        /*@Override
        public boolean mouseClicked(Click click, boolean doubled) {
            if (super.mouseClicked(click, doubled)) {
                return true;
            }
            return selectRowAt(click.x(), click.y());
        }
        *///?} else {
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return selectRowAt(mouseX, mouseY);
        }
        //?}

        //? if >=1.21 {
        /*@Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            return scrollDetails(mouseX, mouseY, verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        *///?} else {
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            return scrollDetails(mouseX, mouseY, amount) || super.mouseScrolled(mouseX, mouseY, amount);
        }
        //?}

        @Override
        public void close() {
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }

        //? if >=1.20 {
        /*@Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            //? if >=1.21 {
            /^this.renderBackground(context, mouseX, mouseY, delta);
            ^///?} else {
            this.renderBackground(context);
            //?}
            renderContent(context);
            suppressBackgroundRender = true;
            try {
                super.render(context, mouseX, mouseY, delta);
            } finally {
                suppressBackgroundRender = false;
            }
        }

        //? if >=1.21 {
        /^@Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            if (suppressBackgroundRender) {
                return;
            }
            super.renderBackground(context, mouseX, mouseY, delta);
        }
        ^///?} else if >=1.20 {
        /^@Override
        public void renderBackground(DrawContext context) {
            if (suppressBackgroundRender) {
                return;
            }
            super.renderBackground(context);
        }
        ^///?}
        *///?} else {
        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderBackground(matrices);
            renderContent(matrices);
            super.render(matrices, mouseX, mouseY, delta);
        }
        //?}

        //? if >=1.20 {
        /*private void renderContent(DrawContext context) {
            int centerX = this.width / 2;
            int controlLabelX = Math.max(20, centerX - 160);

            context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, 16, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Music Delay Length"), controlLabelX, 47, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Music Switch Speed"), controlLabelX, 73, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Debug Mode Enabled"), controlLabelX, 99, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Songpacks"), listLeft(), 144, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal("Details"), detailsLeft(), 144, LABEL_COLOR);
            context.drawTextWithShadow(this.textRenderer, Text.literal(pageLabel()), centerX - this.textRenderer.getWidth(pageLabel()) / 2, this.height - 44, MUTED_COLOR);

            renderSongpackRows(context);
            renderDetails(context);
        }

        private void renderSongpackRows(DrawContext context) {
            int left = listLeft();
            int right = listRight();
            int rowTextRight = right - 86;
            int perPage = rowsPerPage();
            int start = page * perPage;
            int end = Math.min(RMSongpackLoader.availableSongpacks.size(), start + perPage);

            for (int i = start; i < end; i++) {
                SongpackZip songpack = RMSongpackLoader.availableSongpacks.get(i);
                int y = SONGPACK_START_Y + (i - start) * ROW_HEIGHT;
                if (i == selectedIndex) {
                    context.fill(left - 4, y - 3, right, y + ROW_HEIGHT - 5, SELECTED_BG);
                }
                RowText row = rowText(songpack);
                context.drawTextWithShadow(this.textRenderer, fit((i == selectedIndex ? "> " : "  ") + row.name, rowTextRight - left), left, y, row.color);
                context.drawTextWithShadow(this.textRenderer, fit(row.detail, rowTextRight - left), left + 10, y + 12, row.detailColor);
            }
        }

        private void renderDetails(DrawContext context) {
            int left = detailsLeft();
            int right = detailsRight();
            int top = SONGPACK_START_Y - 4;
            int bottom = detailsBottom();
            drawPanel(context, left, top, right, bottom);

            List<OrderedText> lines = detailsLines(right - left - 16);
            int visibleLines = Math.max(1, (bottom - top - 16) / 10);
            detailsScroll = Math.max(0, Math.min(detailsScroll, maxDetailsScroll(lines, visibleLines)));

            int y = top + 8;
            int end = Math.min(lines.size(), detailsScroll + visibleLines);
            for (int i = detailsScroll; i < end; i++) {
                context.drawTextWithShadow(this.textRenderer, lines.get(i), left + 8, y, LABEL_COLOR);
                y += 10;
            }

            if (lines.size() > visibleLines) {
                String scroll = "Scroll " + (detailsScroll + 1) + " / " + (maxDetailsScroll(lines, visibleLines) + 1);
                context.drawTextWithShadow(this.textRenderer, Text.literal(scroll), right - 8 - this.textRenderer.getWidth(scroll), bottom - 12, MUTED_COLOR);
            }
        }

        private void drawPanel(DrawContext context, int left, int top, int right, int bottom) {
            context.fill(left, top, right, bottom, PANEL_BG);
            context.fill(left, top, right, top + 1, PANEL_BORDER);
            context.fill(left, bottom - 1, right, bottom, PANEL_BORDER);
            context.fill(left, top, left + 1, bottom, PANEL_BORDER);
            context.fill(right - 1, top, right, bottom, PANEL_BORDER);
        }
        *///?} else {
        private void renderContent(MatrixStack matrices) {
            int centerX = this.width / 2;
            int controlLabelX = Math.max(20, centerX - 160);

            this.textRenderer.drawWithShadow(matrices, this.title, centerX - this.textRenderer.getWidth(this.title) / 2, 16, LABEL_COLOR);
            this.textRenderer.drawWithShadow(matrices, Text.literal("Music Delay Length"), controlLabelX, 47, LABEL_COLOR);
            this.textRenderer.drawWithShadow(matrices, Text.literal("Music Switch Speed"), controlLabelX, 73, LABEL_COLOR);
            this.textRenderer.drawWithShadow(matrices, Text.literal("Debug Mode Enabled"), controlLabelX, 99, LABEL_COLOR);
            this.textRenderer.drawWithShadow(matrices, Text.literal("Songpacks"), listLeft(), 144, LABEL_COLOR);
            this.textRenderer.drawWithShadow(matrices, Text.literal("Details"), detailsLeft(), 144, LABEL_COLOR);
            String label = pageLabel();
            this.textRenderer.drawWithShadow(matrices, label, centerX - this.textRenderer.getWidth(label) / 2, this.height - 44, MUTED_COLOR);

            renderSongpackRows(matrices);
            renderDetails(matrices);
        }

        private void renderSongpackRows(MatrixStack matrices) {
            int left = listLeft();
            int right = listRight();
            int rowTextRight = right - 86;
            int perPage = rowsPerPage();
            int start = page * perPage;
            int end = Math.min(RMSongpackLoader.availableSongpacks.size(), start + perPage);

            for (int i = start; i < end; i++) {
                SongpackZip songpack = RMSongpackLoader.availableSongpacks.get(i);
                int y = SONGPACK_START_Y + (i - start) * ROW_HEIGHT;
                if (i == selectedIndex) {
                    fill(matrices, left - 4, y - 3, right, y + ROW_HEIGHT - 5, SELECTED_BG);
                }
                RowText row = rowText(songpack);
                this.textRenderer.drawWithShadow(matrices, fit((i == selectedIndex ? "> " : "  ") + row.name, rowTextRight - left), left, y, row.color);
                this.textRenderer.drawWithShadow(matrices, fit(row.detail, rowTextRight - left), left + 10, y + 12, row.detailColor);
            }
        }

        private void renderDetails(MatrixStack matrices) {
            int left = detailsLeft();
            int right = detailsRight();
            int top = SONGPACK_START_Y - 4;
            int bottom = detailsBottom();
            drawPanel(matrices, left, top, right, bottom);

            List<OrderedText> lines = detailsLines(right - left - 16);
            int visibleLines = Math.max(1, (bottom - top - 16) / 10);
            detailsScroll = Math.max(0, Math.min(detailsScroll, maxDetailsScroll(lines, visibleLines)));

            int y = top + 8;
            int end = Math.min(lines.size(), detailsScroll + visibleLines);
            for (int i = detailsScroll; i < end; i++) {
                this.textRenderer.drawWithShadow(matrices, lines.get(i), left + 8, y, LABEL_COLOR);
                y += 10;
            }

            if (lines.size() > visibleLines) {
                String scroll = "Scroll " + (detailsScroll + 1) + " / " + (maxDetailsScroll(lines, visibleLines) + 1);
                this.textRenderer.drawWithShadow(matrices, scroll, right - 8 - this.textRenderer.getWidth(scroll), bottom - 12, MUTED_COLOR);
            }
        }

        private void drawPanel(MatrixStack matrices, int left, int top, int right, int bottom) {
            fill(matrices, left, top, right, bottom, PANEL_BG);
            fill(matrices, left, top, right, top + 1, PANEL_BORDER);
            fill(matrices, left, bottom - 1, right, bottom, PANEL_BORDER);
            fill(matrices, left, top, left + 1, bottom, PANEL_BORDER);
            fill(matrices, right - 1, top, right, bottom, PANEL_BORDER);
        }
        //?}

        private boolean scrollDetails(double mouseX, double mouseY, double amount) {
            if (mouseX < detailsLeft() || mouseX > detailsRight() || mouseY < SONGPACK_START_Y - 4 || mouseY > detailsBottom()) {
                return false;
            }

            int visibleLines = Math.max(1, (detailsBottom() - (SONGPACK_START_Y - 4) - 16) / 10);
            List<OrderedText> lines = detailsLines(detailsRight() - detailsLeft() - 16);
            detailsScroll = Math.max(0, Math.min(detailsScroll - (int) Math.signum(amount) * 3, maxDetailsScroll(lines, visibleLines)));
            return true;
        }

        private List<OrderedText> detailsLines(int width) {
            ArrayList<OrderedText> lines = new ArrayList<>();
            SongpackZip songpack = selectedSongpack();
            if (songpack == null) {
                addWrapped(lines, "No songpacks found.", width);
                return lines;
            }

            addWrapped(lines, songpackName(songpack), width);
            addWrapped(lines, "Status: " + statusText(songpack), width);
            addBlank(lines);

            if (songpack.errorString != null && !songpack.errorString.isEmpty()) {
                addWrapped(lines, songpack.blockLoading ? "Error:" : "Warnings:", width);
                addWrapped(lines, songpack.errorString.trim(), width);
                addBlank(lines);
            }

            String description = songpack.config != null && songpack.config.description != null ? songpack.config.description.trim() : "";
            if (!description.isEmpty()) {
                addWrapped(lines, "Description:", width);
                addWrapped(lines, description, width);
                addBlank(lines);
            }

            String credits = songpack.config != null && songpack.config.credits != null ? songpack.config.credits.trim() : "";
            if (!credits.isEmpty()) {
                addWrapped(lines, "Credits:", width);
                addWrapped(lines, credits, width);
                addBlank(lines);
            }

            if (songpack.path != null) {
                addWrapped(lines, "Path: " + songpack.path, width);
            }

            return lines;
        }

        private void addWrapped(List<OrderedText> lines, String value, int width) {
            if (value == null || value.isEmpty()) {
                addBlank(lines);
                return;
            }
            for (String line : value.split("\\R", -1)) {
                if (line.isEmpty()) {
                    addBlank(lines);
                } else {
                    lines.addAll(this.textRenderer.wrapLines(Text.literal(line), width));
                }
            }
        }

        private void addBlank(List<OrderedText> lines) {
            lines.add(Text.literal(" ").asOrderedText());
        }

        private int maxDetailsScroll(List<OrderedText> lines, int visibleLines) {
            return Math.max(0, lines.size() - visibleLines);
        }

        private int rowsPerPage() {
            return Math.max(1, (this.height - SONGPACK_START_Y - 56) / ROW_HEIGHT);
        }

        private int totalPages(int perPage) {
            int count = RMSongpackLoader.availableSongpacks.size();
            return Math.max(1, (count + perPage - 1) / perPage);
        }

        private int defaultSelectedIndex() {
            for (int i = 0; i < RMSongpackLoader.availableSongpacks.size(); i++) {
                if (isLoaded(RMSongpackLoader.availableSongpacks.get(i))) {
                    return i;
                }
            }
            return RMSongpackLoader.availableSongpacks.isEmpty() ? -1 : 0;
        }

        private int clampSelectedIndex(int index) {
            if (RMSongpackLoader.availableSongpacks.isEmpty()) {
                return -1;
            }
            return Math.max(0, Math.min(index, RMSongpackLoader.availableSongpacks.size() - 1));
        }

        private SongpackZip selectedSongpack() {
            selectedIndex = clampSelectedIndex(selectedIndex);
            return selectedIndex >= 0 ? RMSongpackLoader.availableSongpacks.get(selectedIndex) : null;
        }

        private boolean selectRowAt(double mouseX, double mouseY) {
            int rowIndex = rowIndexAt(mouseX, mouseY);
            if (rowIndex < 0) {
                return false;
            }
            selectedIndex = rowIndex;
            detailsScroll = 0;
            return true;
        }

        private int rowIndexAt(double mouseX, double mouseY) {
            if (mouseX < listLeft() || mouseX > listRight() || mouseY < SONGPACK_START_Y) {
                return -1;
            }
            int row = ((int) mouseY - SONGPACK_START_Y) / ROW_HEIGHT;
            if (row < 0 || row >= rowsPerPage()) {
                return -1;
            }
            int index = page * rowsPerPage() + row;
            return index < RMSongpackLoader.availableSongpacks.size() ? index : -1;
        }

        private int listLeft() {
            return 20;
        }

        private int listRight() {
            return Math.max(330, this.width / 2 + 30);
        }

        private int detailsLeft() {
            return listRight() + 18;
        }

        private int detailsRight() {
            return this.width - 20;
        }

        private int detailsBottom() {
            return this.height - 56;
        }

        private String pageLabel() {
            return "Page " + (page + 1) + " / " + totalPages(rowsPerPage());
        }

        private boolean isLoaded(SongpackZip songpack) {
            return ReactiveMusic.currentSongpack != null
                    && ReactiveMusic.currentSongpack.config != null
                    && songpack.config != null
                    && Objects.equals(ReactiveMusic.currentSongpack.config.name, songpack.config.name);
        }

        private String statusText(SongpackZip songpack) {
            if (songpack.blockLoading) {
                return "Failed loading";
            }
            if (isLoaded(songpack)) {
                return "Loaded";
            }
            boolean showWarnings = getConfig().debugModeEnabled
                    || songpack.isv05OldSongpack
                    || (songpack.path != null && !songpack.path.toString().endsWith(".zip"));
            if (showWarnings && songpack.errorString != null && !songpack.errorString.isEmpty()) {
                return "Warning";
            }
            return "Available";
        }

        private RowText rowText(SongpackZip songpack) {
            String name = songpackName(songpack);
            if (songpack.blockLoading) {
                return new RowText("FAILED LOADING: " + name, firstLine(songpack.errorString), ERROR_COLOR, ERROR_COLOR);
            }

            String description = songpack.config != null && songpack.config.description != null ? songpack.config.description : "";
            boolean showWarnings = getConfig().debugModeEnabled
                    || songpack.isv05OldSongpack
                    || (songpack.path != null && !songpack.path.toString().endsWith(".zip"));
            if (showWarnings && songpack.errorString != null && !songpack.errorString.isEmpty()) {
                return new RowText("WARNING: " + name, firstLine(songpack.errorString), WARNING_COLOR, WARNING_COLOR);
            }

            if (isLoaded(songpack)) {
                description = "Currently loaded" + (description.isEmpty() ? "" : " - " + description);
            }
            return new RowText(name, firstLine(description), LABEL_COLOR, MUTED_COLOR);
        }

        private String songpackName(SongpackZip songpack) {
            if (songpack.config == null || songpack.config.name == null || songpack.config.name.isEmpty()) {
                return "Unknown songpack";
            }
            return songpack.config.name;
        }

        private String firstLine(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            int newline = value.indexOf('\n');
            return newline >= 0 ? value.substring(0, newline) : value;
        }

        private Text fit(String value, int maxWidth) {
            if (value == null || value.isEmpty()) {
                return Text.literal("");
            }
            if (this.textRenderer.getWidth(value) <= maxWidth) {
                return Text.literal(value);
            }
            String suffix = "...";
            int max = Math.max(0, maxWidth - this.textRenderer.getWidth(suffix));
            String trimmed = value;
            while (!trimmed.isEmpty() && this.textRenderer.getWidth(trimmed) > max) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return Text.literal(trimmed + suffix);
        }

        private static void openSongpackFolder() {
            Path path = PlatformHelper.INSTANCE.getGameDir().resolve("resourcepacks");
            try {
                Files.createDirectories(path);
                Util.getOperatingSystem().open(path.toFile());
            } catch (IOException e) {
                ReactiveMusic.LOGGER.error("Failed to open songpack folder", e);
            }
        }

        private static ButtonWidget button(int x, int y, int width, int height, Text text, ButtonWidget.PressAction action) {
            //? if >=1.20 {
            /*return ButtonWidget.builder(text, action).dimensions(x, y, width, height).build();
            *///?} else {
            return new ButtonWidget(x, y, width, height, text, action);
            //?}
        }

        private static Text enumText(Enum<?> value) {
            return Text.literal(value.name());
        }

        private static Text toggleText(boolean value) {
            return Text.literal(value ? "On" : "Off");
        }

        private static <T extends Enum<T>> T next(T current, T[] values) {
            return values[(current.ordinal() + 1) % values.length];
        }

        private static class RowText {
            private final String name;
            private final String detail;
            private final int color;
            private final int detailColor;

            private RowText(String name, String detail, int color, int detailColor) {
                this.name = name;
                this.detail = detail;
                this.color = color;
                this.detailColor = detailColor;
            }
        }
    }

    public static class ConfigStore {
        private static final Path PATH = Paths.get("config", "ReactiveMusic.json5");
        private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        private ModConfig instance = new ModConfig();

        public ModConfig instance() {
            return instance;
        }

        public void load() {
            if (!Files.exists(PATH)) {
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(PATH)) {
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                ModConfig loaded = gson.fromJson(jsonReader, ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    ensureCollections();
                }
            } catch (Exception e) {
                System.err.println("[ReactiveMusic] Failed to load config, using defaults: " + e.getMessage());
                instance = new ModConfig();
            }
        }

        public void save() {
            ensureCollections();
            try {
                Path parent = PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (Writer writer = Files.newBufferedWriter(PATH)) {
                    gson.toJson(instance, writer);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to save Reactive Music config", e);
            }
        }

        private void ensureCollections() {
            if (instance.blacklistedDimensions == null) instance.blacklistedDimensions = new ArrayList<>();
            if (instance.savedHomePositions == null) instance.savedHomePositions = new HashMap<>();
            if (instance.soundsMuteMusic == null) instance.soundsMuteMusic = new ArrayList<>();
            if (instance.soundsMuteMusicIgnoreDistance == null) instance.soundsMuteMusicIgnoreDistance = new ArrayList<>();
            if (instance.loadedUserSongpack == null) instance.loadedUserSongpack = "";
            if (instance.musicDelayLength2 == null) instance.musicDelayLength2 = MusicDelayLength.SONGPACK_DEFAULT;
            if (instance.musicSwitchSpeed2 == null) instance.musicSwitchSpeed2 = MusicSwitchSpeed.SONGPACK_DEFAULT;
        }
    }
}
