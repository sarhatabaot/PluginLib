package com.github.reflxctiondev.pluginlib;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that handles relocation
 */
public abstract class FileRelocator {

    private static final PluginLib asm = PluginLib.builder()
            .groupId("org.ow2.asm")
            .artifactId("asm")
            .version("9.5")
            .build();

    private static final PluginLib asm_commons = PluginLib.builder()
            .groupId("org.ow2.asm")
            .artifactId("asm-commons")
            .version("9.5")
            .build();

    private static final PluginLib jarRelocator = PluginLib.builder()
            .groupId("me.lucko")
            .artifactId("jar-relocator")
            .version("1.7")
            .build();

    private static Constructor<?> relocatorConstructor;
    private static Method relocateMethod;

    /**
     * Loads all the required libraries for relocation
     *
     * @param clazz Class that extends {@link DependentJavaPlugin}.
     */
    public static void load(Class<? extends DependentJavaPlugin> clazz) {
        asm.load(clazz);
        asm_commons.load(clazz);
        jarRelocator.load(clazz);

        try {
            Class<?> reloc = Class.forName("me.lucko.jarrelocator.JarRelocator");
            relocatorConstructor = reloc.getDeclaredConstructor(File.class, File.class, Map.class);
            relocatorConstructor.setAccessible(true);
            relocateMethod = reloc.getDeclaredMethod("run");
            relocateMethod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void remap(File input, File output, @NotNull Set<Relocation> relocations) throws Exception {
        Map<String, String> mappings = new HashMap<>();
        for (Relocation relocation : relocations) {
            mappings.put(relocation.getPath(), relocation.getNewPath());
        }

        // create and invoke a new relocator
        Object relocator = relocatorConstructor.newInstance(input, output, mappings);
        relocateMethod.invoke(relocator);
    }

    private FileRelocator() {
        throw new AssertionError("Cannot create instances of " + getClass().getName() + ".");
    }

}
