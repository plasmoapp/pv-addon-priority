package su.plo.voice.priority;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.voice.api.addon.AddonScope;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.config.VoiceServerConfigLoadedEvent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Addon(
        id = "priority",
        scope = AddonScope.SERVER,
        version = "1.0.0",
        authors = {"Apehum"}
)
public final class PriorityAddon {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    @Getter
    private PriorityConfig config;

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceServerConfigLoadedEvent event) {
        PlasmoVoiceServer voiceServer = event.getServer();

        try {
            File addonFolder = new File(voiceServer.getConfigFolder(), "addons/priority");
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

        PriorityActivation activation = new PriorityActivation(
                voiceServer,
                this
        );
        voiceServer.getEventBus().register(this, activation);
        activation.register();
    }

    private InputStream getLanguageResource(@NotNull String resourcePath) throws IOException {
        return getClass().getClassLoader().getResourceAsStream(String.format("priority/%s", resourcePath));
    }
}
