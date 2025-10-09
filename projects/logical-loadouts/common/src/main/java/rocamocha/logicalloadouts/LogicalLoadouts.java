package com.mochamix.logicalloadouts;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalLoadouts implements ModInitializer {
    public static final String MOD_ID = "logical-loadouts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Logical Loadouts");
        // TODO: Initialize loadout management system
    }
}