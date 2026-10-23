package dev.darkvisuals.security;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class DarkVSPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // проверка совместимости версии LWJGL
        System.setProperty("sodium.checks.issue2561", "false");
        // проверка драйвера Intel (Windows)
        System.setProperty("sodium.checks.issue899", "false");
    }
}
