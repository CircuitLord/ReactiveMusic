package circuitlord.reactivemusic;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactiveMusicDebug {
    public ReactiveMusicDebug() {}
    public static final ReactiveMusicDebug INSTANCE = new ReactiveMusicDebug(); 
    
    public static final String NON_IMPL_WARN = """
        This feature is not implemented yet!
        Track related issues and follow development @
        https://github.com/users/rocamocha/projects
        """;

    public static final Text NON_IMPL_WARN_BUILT = new TextBuilder()
        .line(ReactiveMusicDebug.NON_IMPL_WARN, Formatting.RED, Formatting.BOLD)
        .build();

    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");
    public static final ChangeLogger CHANGE_LOGGER = INSTANCE.new ChangeLogger();

    /**
     * Log categories for fine-grained control with minimal performance impact
     * 
     * TODO: figure out which categories are most useful in practice
     * and refine the list accordingly.
     */
    public enum LogCategory {
        VALIDATION("validation"),
        PLUGIN_EXECUTION("plugins"), 
        EVENT_PROCESSING("events"),
        PERFORMANCE("performance"),
        BLOCK_CHECKING("blocks"),
        BIOME_PROCESSING("biomes"),
        ZONE_VALIDATION("zones"),
        GENERAL("general");
        
        private final String id;
        LogCategory(String id) { this.id = id; }
        public String getId() { return id; }
    }

    // Category management - thread-safe collections for performance
    private static final Set<LogCategory> enabledCategories = ConcurrentHashMap.newKeySet();
    private static final Set<LogCategory> spamPreventionCategories = EnumSet.of(
        LogCategory.VALIDATION, LogCategory.EVENT_PROCESSING, LogCategory.BLOCK_CHECKING
    );
    private static volatile boolean debugMode = true; // Default enabled for development
    
    static {
        // Initialize with all categories enabled
        enabledCategories.addAll(EnumSet.allOf(LogCategory.class));
    }

    /**
     * Low-churn category management methods
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        if (enabled) {
            enabledCategories.addAll(EnumSet.allOf(LogCategory.class));
        } else {
            // Don't clear categories when disabling debug mode - let individual category settings persist
            // This prevents lost messages during mode transitions
        }
        LOGGER.info("Debug mode set to: {}, active categories: {}", enabled, enabledCategories.size());
    }

    public static void enableCategory(LogCategory category) {
        enabledCategories.add(category);
    }

    public static void disableCategory(LogCategory category) {
        enabledCategories.remove(category);
    }

    public static void enableCategories(LogCategory... categories) {
        for (LogCategory cat : categories) {
            enabledCategories.add(cat);
        }
    }

    public static void disableCategories(LogCategory... categories) {
        for (LogCategory cat : categories) {
            enabledCategories.remove(cat);
        }
    }

    public static boolean isEnabled(LogCategory category) {
        return debugMode && enabledCategories.contains(category);
    }

    /**
     * Convenience method for categorized logging - minimal churn to existing code
     */
    public static void log(LogCategory category, String message) {
        try {
            if (isEnabled(category)) {
                if (spamPreventionCategories.contains(category)) {
                    CHANGE_LOGGER.writeInfo("[" + category.getId() + "] " + message);
                } else {
                    LOGGER.info("[{}] {}", category.getId(), message);
                }
            }
        } catch (Exception e) {
            // Fallback logging to prevent logger failures from breaking the game
            System.err.println("[LOGGER_ERROR] Failed to log message for category " + category + ": " + message);
            e.printStackTrace();
        }
    }

    /**
     * Health check and diagnostic methods
     */
    public static void logDiagnostics() {
        LOGGER.info("=== Logging Diagnostics ===");
        LOGGER.info("Debug mode: {}", debugMode);
        LOGGER.info("Enabled categories: {}", enabledCategories);
        LOGGER.info("Spam prevention categories: {}", spamPreventionCategories);
        LOGGER.info("Change logger cache size: {}", CHANGE_LOGGER.categoryCache.size());
    }

    /**
     * Useful for monitoring changes on an assignment that happens every tick,
     * without causing the console to flail about.
     * 
     * Enhanced with category support for performance tuning while maintaining
     * existing behavior for compatibility.
     * 
     * TODO: Implement a history function with queries to tick or repeat values, etc.
     * TODO: Implement realtime tracking.
     */
    public class ChangeLogger {

        private NewLog PreviousLog = null;
        // Category-specific previous logs for finer spam prevention
        private final ConcurrentHashMap<LogCategory, NewLog> categoryPreviousLogs = new ConcurrentHashMap<>();
        private static final int MAX_CATEGORY_LOGS = 50; // Prevent memory bloat
        private volatile int logCount = 0;
        
        public static enum LogType { INFO, ERROR, DEBUG } 

        private class NewLog {

            private LogType logType;
            private String msg;
            private Throwable throwable;
            private LogCategory category;

            private NewLog(String msg) {
                this.logType = LogType.INFO;
                this.msg = msg;
                this.throwable = null;
                this.category = null;
            }

            private NewLog(String msg, Throwable throwable) {
                this.logType = LogType.ERROR;
                this.msg = msg;
                this.throwable = throwable;
                this.category = null;
            }

            private NewLog(LogCategory category, String msg) {
                this.logType = LogType.INFO;
                this.msg = msg;
                this.throwable = null;
                this.category = category;
            }

            @Override public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof NewLog)) return false;
                NewLog that = (NewLog) o;
                return logType == that.logType
                    && Objects.equals(msg, that.msg)
                    && Objects.equals(throwable, that.throwable)
                    && Objects.equals(category, that.category);
            }

            @Override public int hashCode() {
                return Objects.hash(logType, msg, throwable, category);
            }
        }

        /**
         * Original methods - unchanged for compatibility
         */
        public void writeError(String msg, Throwable throwable) {
            NewLog thisLog = new NewLog(msg, throwable);
            if (thisLog.equals(PreviousLog)) return;
            LOGGER.error(msg, throwable);
            PreviousLog = thisLog;
        }
        
        public void writeInfo(String msg) {
            NewLog thisLog = new NewLog(msg);
            if (thisLog.equals(PreviousLog)) return;
            LOGGER.info(msg);
            PreviousLog = thisLog;
        }

        /**
         * New categorized methods - minimal churn additions
         */
        public void writeInfo(LogCategory category, String msg) {
            if (!isEnabled(category)) return;
            
            NewLog thisLog = new NewLog(category, msg);
            NewLog previousCategoryLog = categoryPreviousLogs.get(category);
            
            if (thisLog.equals(previousCategoryLog)) return;
            
            LOGGER.info("[{}] {}", category.getId(), msg);
            categoryPreviousLogs.put(category, thisLog);
            
            // Prevent memory bloat - periodically clear old logs
            if (++logCount > MAX_CATEGORY_LOGS) {
                categoryPreviousLogs.clear();
                logCount = 0;
            }
        }

        /**
         * Convenience method that works with existing writeInfo calls
         * Automatically extracts category from message if present
         */
        public void writeInfoSmart(String msg) {
            LogCategory detectedCategory = detectCategory(msg);
            if (detectedCategory != null) {
                writeInfo(detectedCategory, msg);
            } else {
                writeInfo(msg); // Fall back to original method
            }
        }

        // Cache for detected categories to avoid repeated string operations
        private final ConcurrentHashMap<String, LogCategory> categoryCache = new ConcurrentHashMap<>();
        private static final int MAX_CACHE_SIZE = 100;

        private LogCategory detectCategory(String msg) {
            // Check cache first for performance
            LogCategory cached = categoryCache.get(msg);
            if (cached != null) return cached;
            
            // Prevent cache from growing too large
            if (categoryCache.size() >= MAX_CACHE_SIZE) {
                categoryCache.clear();
            }
            
            // Optimized detection - avoid toLowerCase() and use indexOf for better performance
            LogCategory detected = detectCategoryOptimized(msg);
            if (detected != null) {
                categoryCache.put(msg, detected);
            }
            return detected;
        }

        private LogCategory detectCategoryOptimized(String msg) {
            // Use indexOf instead of toLowerCase().contains() for better performance
            if (containsIgnoreCase(msg, "validat")) return LogCategory.VALIDATION;
            if (containsIgnoreCase(msg, "event")) return LogCategory.EVENT_PROCESSING;
            if (containsIgnoreCase(msg, "block")) return LogCategory.BLOCK_CHECKING;
            if (containsIgnoreCase(msg, "biome")) return LogCategory.BIOME_PROCESSING;
            if (containsIgnoreCase(msg, "zone")) return LogCategory.ZONE_VALIDATION;
            if (containsIgnoreCase(msg, "plugin")) return LogCategory.PLUGIN_EXECUTION;
            return null;
        }

        private boolean containsIgnoreCase(String str, String searchStr) {
            return str.regionMatches(true, 0, searchStr, 0, searchStr.length()) ||
                   str.toLowerCase().indexOf(searchStr.toLowerCase()) >= 0;
        }
    }

    public class Wrapper {
        private static ChangeLogger WRAPPER_LOGGER = INSTANCE.new ChangeLogger();

        public static void fn(Runnable m) {
            WRAPPER_LOGGER.writeInfo(Thread.currentThread().getStackTrace()[2].getMethodName());
            m.run();
        }
    }

    /**
     * Preconstruction of string literals with formatting for in-game debugging convenience.
     * 
     * XXX ~ rocamocha ~ This idea should honestly be its own library - this is a self-reminder extract it and expand!
     */
    public static class TextBuilder {
        protected final MutableText root;

        public TextBuilder() {
            this.root = Text.empty();
        }

        public TextBuilder header(String text) {
            root.append(
                Text.literal("====== " + text + " ======\n\n")
                .formatted(Formatting.GOLD, Formatting.BOLD)
            );
            
            return this;
        }

        public TextBuilder line(String value, Formatting... formats) {
            root.append(Text.literal(value).formatted(formats));
            root.append(Text.literal("\n"));
            return this;
        }
        
        public TextBuilder line(String label, String value, Formatting valueColor) {
            root.append(Text.literal(label + ": ").formatted(Formatting.YELLOW));
            root.append(Text.literal(value).formatted(valueColor, Formatting.BOLD));
            root.append(Text.literal("\n"));
            return this;
        }
        
        public TextBuilder raw(String text, Formatting... formats) {
            root.append(Text.literal(text).formatted(formats));
            return this;
        }

        public TextBuilder newline() {
            root.append(Text.literal("\n"));
            return this;
        }

        public MutableText build() {
            return root;
        }

    }
}
