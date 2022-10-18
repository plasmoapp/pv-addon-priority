package su.plo.voice.priority;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.config.provider.ConfigurationProvider;
import su.plo.config.provider.toml.TomlConfiguration;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.capture.ServerActivationManager;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.line.ServerSourceLineManager;
import su.plo.voice.api.server.audio.source.ServerPlayerSource;
import su.plo.voice.api.server.event.VoiceServerConfigLoadedEvent;
import su.plo.voice.api.server.event.VoiceServerInitializeEvent;
import su.plo.voice.api.server.event.audio.capture.ServerActivationRegisterEvent;
import su.plo.voice.api.server.event.audio.capture.ServerActivationUnregisterEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.proto.data.audio.capture.VoiceActivation;
import su.plo.voice.proto.data.audio.line.VoiceSourceLine;
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Addon(id = "priority", scope = Addon.Scope.SERVER, version = "1.0.0", authors = {"Apehum"})
public final class PriorityAddon {

    private static final ConfigurationProvider toml = ConfigurationProvider.getProvider(TomlConfiguration.class);

    private static final String ACTIVATION_NAME = "priority";
    private static final UUID ACTIVATION_ID = VoiceActivation.generateId(ACTIVATION_NAME);
    private static final UUID SOURCE_LINE_ID = VoiceSourceLine.generateId(ACTIVATION_NAME);

    private PlasmoVoiceServer voiceServer;
    private PriorityConfig config;

    @EventSubscribe
    public void onInitialize(@NotNull VoiceServerInitializeEvent event) {
        this.voiceServer = event.getServer();
    }

    @EventSubscribe
    public void onConfigLoaded(@NotNull VoiceServerConfigLoadedEvent event) {
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

        PlasmoVoiceServer voiceServer = event.getServer();

        ServerActivationManager activationManager = voiceServer.getActivationManager();
        ServerSourceLineManager sourceLineManager = voiceServer.getSourceLineManager();

        activationManager.register(
                this,
                ACTIVATION_NAME,
                "activation.plasmovoice.priority",
                "plasmovoice:textures/icons/microphone_priority.png",
                ImmutableList.of(-1, config.getMaxDistance()),
                config.getDefaultDistance(),
                false,
                true,
                config.getActivationWeight()
        );

        sourceLineManager.register(
                this,
                ACTIVATION_NAME,
                "activation.plasmovoice.priority",
                "plasmovoice:textures/icons/speaker_priority.png",
                config.getSourceLineWeight()
        );
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onActivationRegister(@NotNull ServerActivationRegisterEvent event) {
        if (event.isCancelled()) return;

        ServerActivation activation = event.getActivation();
        if (!activation.getName().equals(ACTIVATION_NAME)) return;

        voiceServer.getMinecraftServer()
                .getPermissionsManager()
                .register("voice.activation." + activation.getName(), PermissionDefault.OP);
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onActivationUnregister(@NotNull ServerActivationUnregisterEvent event) {
        if (event.isCancelled()) return;

        ServerActivation activation = event.getActivation();
        if (!activation.getName().equals(ACTIVATION_NAME)) return;

        voiceServer.getMinecraftServer()
                .getPermissionsManager()
                .unregister("voice.activation." + activation.getName());
    }

    @EventSubscribe
    public void onPlayerSpeak(@NotNull PlayerSpeakEvent event) {
        VoicePlayer player = event.getPlayer();
        PlayerAudioPacket packet = event.getPacket();

        if (!player.getInstance().hasPermission("voice.activation." + ACTIVATION_NAME) ||
                packet.getDistance() > config.getMaxDistance() ||
                packet.getDistance() <= 0
        ) return;

        getPlayerSource(player, packet.getActivationId(), packet.isStereo()).ifPresent((source) -> {
            SourceAudioPacket sourcePacket = new SourceAudioPacket(
                    packet.getSequenceNumber(),
                    (byte) source.getState(),
                    packet.getData(),
                    source.getId(),
                    packet.getDistance()
            );
            source.sendAudioPacket(sourcePacket, packet.getDistance());
        });
    }

    @EventSubscribe
    public void onPlayerSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        VoicePlayer player = event.getPlayer();
        PlayerAudioEndPacket packet = event.getPacket();

        if (!player.getInstance().hasPermission("voice.activation." + ACTIVATION_NAME) ||
                packet.getDistance() > config.getMaxDistance() ||
                packet.getDistance() <= 0
        ) return;

        getPlayerSource(player, packet.getActivationId(), true).ifPresent((source) -> {
            SourceAudioEndPacket sourcePacket = new SourceAudioEndPacket(source.getId(), packet.getSequenceNumber());
            source.sendPacket(sourcePacket, packet.getDistance());
        });
    }

    private Optional<ServerPlayerSource> getPlayerSource(@NotNull VoicePlayer player,
                                                         @NotNull UUID activationId,
                                                         boolean isStereo) {
        if (!activationId.equals(ACTIVATION_ID)) return Optional.empty();

        Optional<ServerActivation> activation = voiceServer.getActivationManager()
                .getActivationById(activationId);
        if (!activation.isPresent()) return Optional.empty();

        Optional<ServerSourceLine> sourceLine = voiceServer.getSourceLineManager()
                .getLineById(SOURCE_LINE_ID);
        if (!sourceLine.isPresent()) return Optional.empty();

        isStereo = isStereo && activation.get().isStereoSupported();
        ServerPlayerSource source = voiceServer.getSourceManager().createPlayerSource(
                voiceServer,
                player,
                sourceLine.get(),
                "opus",
                isStereo
        );
        source.setLine(sourceLine.get());
        source.setStereo(isStereo);

        return Optional.of(source);
    }
}
