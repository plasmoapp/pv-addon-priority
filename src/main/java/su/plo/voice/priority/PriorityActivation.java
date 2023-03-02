package su.plo.voice.priority;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.BaseProximityServerActivation;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.source.ServerPlayerSource;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

public final class PriorityActivation extends BaseProximityServerActivation {

    private final PriorityAddon addon;
    private final PriorityConfig config;

    private ServerActivation activation;

    public PriorityActivation(@NotNull PlasmoVoiceServer voiceServer,
                              @NotNull PriorityAddon addon) {
        super(voiceServer, "priority", PermissionDefault.OP);

        this.addon = addon;
        this.config = addon.getConfig();
    }

    public void register() {
        ServerActivation.Builder builder = getVoiceServer().getActivationManager().createBuilder(
                addon,
                getActivationName(),
                "pv.activation.priority",
                "plasmovoice:textures/icons/microphone_priority.png",
                "pv.activation.priority",
                config.activationWeight()
        );
        this.activation = builder
                .setDistances(ImmutableList.of(-1, config.maxDistance()))
                .setDefaultDistance(config.defaultDistance())
                .setProximity(true)
                .setTransitive(false)
                .setStereoSupported(config.activationStereoSupport())
                .build();

        getVoiceServer().getSourceLineManager().register(
                addon,
                getActivationName(),
                "pv.activation.priority",
                "plasmovoice:textures/icons/speaker_priority.png",
                config.sourceLineWeight()
        );
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeak(@NotNull PlayerSpeakEvent event) {
        if (activation == null) return;

        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        PlayerAudioPacket packet = event.getPacket();

        if (!activation.checkPermissions(player) ||
                packet.getDistance() > config.maxDistance() ||
                packet.getDistance() <= 0
        ) return;

        ServerPlayerSource source = getPlayerSource(player, packet.getActivationId(), packet.isStereo());
        if (source == null) return;

        sendAudioPacket(player, source, packet);
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        if (activation == null) return;

        VoiceServerPlayer player = (VoiceServerPlayer) event.getPlayer();
        PlayerAudioEndPacket packet = event.getPacket();

        if (!activation.checkPermissions(player) ||
                packet.getDistance() > config.maxDistance() ||
                packet.getDistance() <= 0
        ) return;

        ServerPlayerSource source = getPlayerSource(player, packet.getActivationId(), null);
        if (source == null) return;

        sendAudioEndPacket(source, packet);
    }
}
