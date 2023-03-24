package su.plo.voice.priority;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.server.permission.PermissionDefault;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ProximityServerActivationHelper;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;

public final class PriorityActivation  {

    private static final String ACTIVATION_NAME = "priority";

    private final PlasmoVoiceServer voiceServer;

    private final PriorityAddon addon;

    private ProximityServerActivationHelper proximityHelper;

    public PriorityActivation(@NotNull PlasmoVoiceServer voiceServer,
                              @NotNull PriorityAddon addon) {
        this.voiceServer = voiceServer;
        this.addon = addon;
    }

    public void register() {
        PriorityConfig config = addon.getConfig();

        voiceServer.getActivationManager().unregister(ACTIVATION_NAME);
        voiceServer.getSourceLineManager().unregister(ACTIVATION_NAME);
        if (proximityHelper != null) voiceServer.getEventBus().unregister(addon, proximityHelper);

        ServerActivation.Builder builder = voiceServer.getActivationManager().createBuilder(
                addon,
                ACTIVATION_NAME,
                "pv.activation.priority",
                "plasmovoice:textures/icons/microphone_priority.png",
                "pv.activation.priority",
                config.activationWeight()
        );
        ServerActivation activation = builder
                .setDistances(ImmutableList.of(-1, config.maxDistance()))
                .setDefaultDistance(config.defaultDistance())
                .setProximity(true)
                .setTransitive(false)
                .setStereoSupported(config.activationStereoSupport())
                .setPermissionDefault(PermissionDefault.OP)
                .build();

        ServerSourceLine sourceLine = voiceServer.getSourceLineManager().createBuilder(
                addon,
                ACTIVATION_NAME,
                "pv.activation.priority",
                "plasmovoice:textures/icons/speaker_priority.png",
                config.sourceLineWeight()
        ).build();

        this.proximityHelper = new ProximityServerActivationHelper(
                voiceServer,
                activation,
                sourceLine,
                null
        );
        voiceServer.getEventBus().register(addon, proximityHelper);
    }
}
