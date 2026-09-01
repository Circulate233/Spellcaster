package com.spellcaster;

import java.io.File;
import java.util.Arrays;

import net.minecraftforge.common.config.Configuration;

public class Config {

    private static final String CLIENT_ONLY = "clientOnly";
    private static final int CLIENT_CAPACITY = 21;

    private static Configuration configuration;
    public static boolean clientOnlyMode;
    public static int[] clientSequence = new int[CLIENT_CAPACITY];
    public static int clientCursor;
    public static int clientMode;

    public static void synchronizeConfiguration(File configFile) {
        configuration = new Configuration(configFile);
        configuration.load();

        clientOnlyMode = configuration.getBoolean(
            "clientOnlyMode",
            Configuration.CATEGORY_GENERAL,
            false,
            "Do not register Spellcaster items or its custom network.");

        String defaultSequence = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
        String sequence = configuration
            .getString("sequence", CLIENT_ONLY, defaultSequence, "Comma-separated ManaMetal skill ids.");
        clientSequence = parseSequence(sequence);
        clientCursor = configuration
            .getInt("cursor", CLIENT_ONLY, 0, 0, CLIENT_CAPACITY - 1, "Current skill sequence cursor.");
        clientMode = configuration.getInt("mode", CLIENT_ONLY, 0, 0, 2, "0=fate normal, 1=fate strict, 2=chaos.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static void saveClientState() {
        if (configuration == null) {
            return;
        }

        configuration.get(CLIENT_ONLY, "sequence", "")
            .set(formatSequence(clientSequence));
        configuration.get(CLIENT_ONLY, "cursor", 0)
            .set(normalizeCursor(clientCursor));
        configuration.get(CLIENT_ONLY, "mode", 0)
            .set(Math.max(0, Math.min(2, clientMode)));
        configuration.save();
    }

    private static int[] parseSequence(String value) {
        int[] result = new int[CLIENT_CAPACITY];
        Arrays.fill(result, -1);
        String[] values = value.split(",");
        for (int i = 0; i < result.length && i < values.length; ++i) {
            try {
                int skill = Integer.parseInt(values[i].trim());
                result[i] = isSkillIdValid(skill) ? skill : -1;
            } catch (NumberFormatException ignored) {
                result[i] = -1;
            }
        }
        return result;
    }

    private static String formatSequence(int[] sequence) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < CLIENT_CAPACITY; ++i) {
            if (i > 0) {
                result.append(',');
            }
            int skillId = sequence != null && i < sequence.length ? sequence[i] : -1;
            result.append(isSkillIdValid(skillId) ? skillId : -1);
        }
        return result.toString();
    }

    private static int normalizeCursor(int cursor) {
        int value = cursor % CLIENT_CAPACITY;
        return value < 0 ? value + CLIENT_CAPACITY : value;
    }

    private static boolean isSkillIdValid(int skillId) {
        int tier = skillId / 100;
        int slot = skillId % 100;
        return tier >= 1 && tier <= 3 && slot >= 0 && slot < 7;
    }
}
