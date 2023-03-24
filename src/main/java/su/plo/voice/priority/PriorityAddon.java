package su.plo.voice.priority;

import com.google.inject.Inject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Addon(
        id = "pv-addon-priority",
        scope = AddonLoaderScope.SERVER,
        version = "1.0.0",
        authors = {"Apehum"}
)
public final class PriorityAddon implements AddonInitializer {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    @Inject
    private PlasmoVoiceServer voiceServer;

    @Getter
    private PriorityConfig config;

    private PriorityActivation activation;

    @Override
    public void onAddonInitialize() {
        loadConfig();
    }

    @EventSubscribe
    public void onConfigReloaded(@NotNull VoiceServerConfigReloadedEvent event) {
        loadConfig();
    }

    private void loadConfig() {
        try {
            File addonFolder = new File(voiceServer.getConfigsFolder(), "pv-addon-priority");
            File configFile = new File(addonFolder, "priority.toml");

            this.config = toml.load(PriorityConfig.class, configFile, false);

            if (config.defaultDistance() > config.maxDistance()) {
                config.defaultDistance(config.maxDistance());
                toml.save(PriorityConfig.class, config, configFile);
            }

            addonFolder.mkdirs();
            toml.save(PriorityConfig.class, config, configFile);

            voiceServer.getLanguages().register(
                    "plasmo-voice-addons",
                    "server/priority.toml",
                    this::getLanguageResource,
                    new File(addonFolder, "languages")
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config", e);
        }

        if (activation == null) {
            this.activation = new PriorityActivation(
                    voiceServer,
                    this
            );
            voiceServer.getEventBus().register(this, activation);
        }

        activation.register();
    }

    private InputStream getLanguageResource(@NotNull String resourcePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(String.format("priority/%s", resourcePath));
    }
}
