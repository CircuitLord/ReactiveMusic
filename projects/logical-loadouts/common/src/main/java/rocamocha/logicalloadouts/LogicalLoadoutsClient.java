package com.mochamix.logicalloadouts;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalLoadoutsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LogicalLoadouts.MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Logical Loadouts Client");
        // TODO: Initialize client-side loadout management features
    }
}