package com.cursee.disenchanting_table;

import com.cursee.monolib.platform.Services;
import com.cursee.monolib.util.toml.Toml;
import com.cursee.monolib.util.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigNeoForge {

    public static final File CONFIG_DIRECTORY = new File(Services.PLATFORM.getGameDirectory() + File.separator + "config");
    public static final String CONFIG_FILEPATH = CONFIG_DIRECTORY + File.separator + Constants.MOD_ID + ".toml";

    // local default
    public static boolean experienceIsRequired = true;

    public static final Map<String, Object> defaults = new HashMap<String, Object>();

    public static void initialize() {

        defaults.put("experienceIsRequired", experienceIsRequired);
        DisenchantingTable.experienceIsRequired = experienceIsRequired;

        if (!CONFIG_DIRECTORY.isDirectory()) {
            CONFIG_DIRECTORY.mkdir();
        }

        final File CONFIG_FILE = new File(CONFIG_FILEPATH);

        ConfigNeoForge.handle(CONFIG_FILE);
    }

    public static void handle(File file) {

        final boolean FILE_NOT_FOUND = !file.isFile();

        if (FILE_NOT_FOUND) {

            try {
                TomlWriter writer = new TomlWriter();
                writer.write(defaults, file);
            }
            catch (IOException exception) {
                Constants.LOG.error("Fatal error occurred while attempting to write " + Constants.MOD_ID + ".toml");
                Constants.LOG.error("Did another process delete the config directory during writing?");
                Constants.LOG.error(exception.getMessage());
            }
        }
        else {

            try {

                Toml toml = new Toml().read(file);
                experienceIsRequired = toml.getBoolean("experienceIsRequired");
                DisenchantingTable.experienceIsRequired =  experienceIsRequired;

            }
            catch (IllegalStateException exception) {
                Constants.LOG.error("Fatal error occurred while attempting to read " + Constants.MOD_ID + ".toml");
                Constants.LOG.error("Did another process delete the file during reading?");
                Constants.LOG.error(exception.getMessage());
            }
        }
    }
}
