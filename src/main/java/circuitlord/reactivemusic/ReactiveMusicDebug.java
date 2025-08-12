package circuitlord.reactivemusic;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReactiveMusicDebug {
    public ReactiveMusicDebug() {}
    public static final ReactiveMusicDebug INSTANCE = new ReactiveMusicDebug(); 
    
    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");
    public static final ChangeLogger CHANGE_LOGGER = INSTANCE.new ChangeLogger();

    /**
     * Useful for monitoring changes on an assignment that happens every tick,
     * without causing the console to flail about.
     * 
     * TODO: Implement a history function with queries to tick or repeat values, etc.
     * TODO: Implement realtime tracking.
     */
    public class ChangeLogger {

        private NewLog PreviousLog = null;
        
        public static enum LogType { INFO, ERROR, DEBUG } 

        private class NewLog {

            private LogType logType;
            private String msg;
            private Throwable throwable;

            private NewLog(String msg) {
                this.logType = LogType.INFO;
                this.msg = msg;
                this.throwable = null;
            }

            private NewLog(String msg, Throwable throwable) {
                this.logType = LogType.ERROR;
                this.msg = msg;
                this.throwable = throwable;
            }

            @Override public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof NewLog)) return false;
                NewLog that = (NewLog) o;
                return logType == that.logType
                    && Objects.equals(msg, that.msg)
                    && Objects.equals(throwable, that.throwable);
            }

            @Override public int hashCode() {
                return Objects.hash(logType, msg, throwable);
            }
        }

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
    public class TextBuilder {
        private final MutableText root;

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

        public MutableText build() {
            return root;
        }

    }






}
