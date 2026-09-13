package dev.darkvisuals.util;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.models.User;
import dev.firstdark.rpc.handlers.RPCEventHandler;

public class DiscordRichPresenceUtil {
     
    private static final String DEFAULT_APP_ID = "1524636505639878706";

    private static volatile boolean running = false;
    private static Thread rpcThread;
    private static DiscordRpc rpc;

    public static String state;

    public static synchronized void discordrpc() {
        startDiscord(null);
    }

    public static synchronized void startDiscord(String applicationId) {
        if (running) return;
        String appId = applicationId;
        if (appId == null || appId.isEmpty()) {
            appId = System.getProperty("discord.app.id", System.getenv("DISCORD_APP_ID"));
        }
        if (appId == null || appId.isEmpty()) appId = DEFAULT_APP_ID;

        rpc = new DiscordRpc();
        rpc.setDebugMode(false);

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                running = true;
                pushPresence();
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                running = false;
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
            }
        };

        try {
            rpc.init(appId, handler, false);
        } catch (Throwable t) {
            return;
        }

        rpcThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (running) pushPresence();
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "Discord-RPC-FirstDark-Thread");
        rpcThread.setDaemon(true);
        rpcThread.start();
    }

    public static synchronized void shutdownDiscord() {
        running = false;
        Thread thread = rpcThread;
        rpcThread = null;
        if (thread != null) {
            thread.interrupt();
            try { thread.join(1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        try { if (rpc != null) rpc.shutdown(); } catch (Throwable ignored) {}
        rpc = null;
    }

    private static void pushPresence() {
        if (rpc == null) return;
        DiscordRichPresence presence = DiscordRichPresence.builder()
                .details(state != null && !state.isEmpty() ? state : "Самый лучший пвп мод на 1.21.4 fabric")

                 
                .largeImageKey("logo_large")
                .largeImageText("Dark Visuals Client")

                 
                .smallImageKey("logo_small")
                .smallImageText("Playing")

                .activityType(ActivityType.PLAYING)
                .button(DiscordRichPresence.RPCButton.of("Скачать", "https://t.me/DarkVisualsi"))
                .build();
        try { rpc.updatePresence(presence); } catch (Throwable ignored) {}
    }
}