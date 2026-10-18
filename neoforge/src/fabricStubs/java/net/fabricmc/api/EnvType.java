package net.fabricmc.api;

/**
 * ZP5: compile-only stand-in for Fabric Loader's EnvType, so upstream's {@code @Environment} markers compile on
 * NeoForge without editing every file. Never packaged: the annotation is CLASS-retained and the JVM ignores annotation
 * types it cannot load.
 */
public enum EnvType {
    CLIENT,
    SERVER
}
