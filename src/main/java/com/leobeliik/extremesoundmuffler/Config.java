package com.leobeliik.extremesoundmuffler;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.leobeliik.extremesoundmuffler.utils.eventHandlers.SoundEventHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber
public class Config {

    static final ForgeConfigSpec CLIENT_CONFIG;
    private static final String CATEGORY_GENERAL = "general";
    private static final ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> forbiddenSounds;
    private static final ForgeConfigSpec.BooleanValue disableInventoryButton;
    private static final ForgeConfigSpec.BooleanValue disableAnchors;
    private static final ForgeConfigSpec.DoubleValue defaultMuteVolume;

    static {
        CLIENT_BUILDER.comment("general settings").push(CATEGORY_GENERAL);

        forbiddenSounds = CLIENT_BUILDER.comment("Blacklisted Sounds - add the name of the sounds to blacklist, separated with comma")
                .defineList("forbiddenSounds", Arrays.asList("ui.", "music.", "ambient."), o -> o instanceof String);

        disableInventoryButton = CLIENT_BUILDER.comment("Disable the Muffle button in the player inventory?")
                .define("disableInventoryButton", false);

        disableAnchors = CLIENT_BUILDER.comment("Disable the anchors?").define("disableAnchors", false);

        defaultMuteVolume = CLIENT_BUILDER.comment("Volume set when pressed the mute button").defineInRange("defaultMuteVolume", 0, 0, 0.9);

        CLIENT_BUILDER.pop();
        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    @SuppressWarnings("SameParameterValue")
    static void loadConfig(ForgeConfigSpec spec, Path path) {
        final CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();

        configData.load();
        spec.setConfig(configData);
        SoundEventHandler.forbiddenSounds().addAll(forbiddenSounds.get());
    }

    static boolean getDisableInventoryButton() {
        return disableInventoryButton.get();
    }

    public static boolean getDisableAchors() {
        return disableAnchors.get();
    }

    public static double getDefaultMuteVolume() {
        return defaultMuteVolume.get();
    }
}
