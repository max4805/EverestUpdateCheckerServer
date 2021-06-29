package com.max480.everest.updatechecker;

import java.util.Map;

public class ServerConfig {
    public static class BananaMirrorConfig {
        public final String knownHosts;
        public final String serverAddress;
        public final String username;
        public final String password;
        public final String directory;
        public final String imagesDirectory;

        public BananaMirrorConfig(Map<String, Object> config) {
            knownHosts = config.get("KnownHosts").toString();
            serverAddress = config.get("ServerAddress").toString();
            username = config.get("Username").toString();
            password = config.get("Password").toString();
            directory = config.get("Directory").toString();
            imagesDirectory = config.get("ImagesDirectory").toString();
        }
    }

    public final boolean mainServerIsMirror;
    public final boolean forceRegisterMods;
    public final BananaMirrorConfig bananaMirrorConfig;

    public ServerConfig(Map<String, Object> config) {
        mainServerIsMirror = (boolean) config.getOrDefault("MainServerIsMirror", false);
        forceRegisterMods = (boolean) config.getOrDefault("ForceRegisterMods", false);
        bananaMirrorConfig = config.containsKey("BananaMirrorConfig") ?
                new BananaMirrorConfig((Map<String, Object>) config.get("BananaMirrorConfig")) : null;
    }
}
