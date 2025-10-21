package rocamocha.voidloom;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for PROJECT_NAME_HERE
 * 
 * This is a template class for new projects in the MochaMix multi-project system.
 * Replace PROJECT_ID_HERE and PROJECT_NAME_HERE with your actual project details.
 * 
 * Place this file in:
 * - projects/[project]/common/src/main/java/rocamocha/[project]/ (for shared code)
 * - projects/[project]/v1_21_1/src/main/java/rocamocha/[project]/ (for version-specific code)
 */
public class Voidloom implements ModInitializer {
    public static final String MOD_ID = "voidloom";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Voidloom...");
        
        VoidloomDimensionBootstrap.registerLifecycleCallbacks();
        
        LOGGER.info("Voidloom initialized successfully!");
    }
}
