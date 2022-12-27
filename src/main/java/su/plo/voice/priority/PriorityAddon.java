package su.plo.voice.priority;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.event.VoiceServerConfigLoadedEvent;

import java.io.File;
import java.io.IOException;

@Addon(id = "priority", scope = Addon.Scope.SERVER, version = "1.0.0", authors = {"Apehum"})
public final class PriorityAddon {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    @Getter
    private PriorityConfig config;

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceServerConfigLoadedEvent event) {
        PlasmoVoiceServer voiceServer = event.getServer();

        try {
            File addonFolder = new File(voiceServer.getConfigFolder(), "addons");
            File configFile = new File(addonFolder, "priority.toml");

            this.config = toml.load(PriorityConfig.class, configFile, false);

            if (config.getDefaultDistance() > config.getMaxDistance()) {
                config.setDefaultDistance(config.getMaxDistance());
                toml.save(PriorityConfig.class, config, configFile);
            }

            toml.save(PriorityConfig.class, config, configFile);
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
}
