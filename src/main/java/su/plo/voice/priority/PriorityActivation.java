package su.plo.voice.priority;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.BaseProximityServerActivation;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

public final class PriorityActivation extends BaseProximityServerActivation {

    private final PriorityAddon addon;
    private final PriorityConfig config;

    public PriorityActivation(@NotNull PlasmoVoiceServer voiceServer,
                              @NotNull PriorityAddon addon) {
        super(voiceServer, "priority", PermissionDefault.OP);

        this.addon = addon;
        this.config = addon.getConfig();
    }

    public void register() {
        voiceServer.getActivationManager().register(
                addon,
                activationName,
                "activation.plasmovoice.priority",
                "plasmovoice:textures/icons/microphone_priority.png",
                ImmutableList.of(-1, config.getMaxDistance()),
                config.getDefaultDistance(),
                false,
                true,
                config.getActivationWeight()
        );

        voiceServer.getSourceLineManager().register(
                addon,
                activationName,
                "activation.plasmovoice.priority",
                "plasmovoice:textures/icons/speaker_priority.png",
                config.getSourceLineWeight()
        );
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeak(@NotNull PlayerSpeakEvent event) {
        VoicePlayer player = event.getPlayer();
        PlayerAudioPacket packet = event.getPacket();

        if (!player.getInstance().hasPermission(getActivationPermission()) ||
                packet.getDistance() > config.getMaxDistance() ||
                packet.getDistance() <= 0
        ) return;

        getPlayerSource(player, packet.getActivationId(), packet.isStereo())
                .ifPresent((source) -> sendAudioPacket(player, source, packet));
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerSpeakEnd(@NotNull PlayerSpeakEndEvent event) {
        VoicePlayer player = event.getPlayer();
        PlayerAudioEndPacket packet = event.getPacket();

        if (!player.getInstance().hasPermission(getActivationPermission()) ||
                packet.getDistance() > config.getMaxDistance() ||
                packet.getDistance() <= 0
        ) return;

        getPlayerSource(player, packet.getActivationId(), true)
                .ifPresent((source) -> sendAudioEndPacket(source, packet));
    }
}
