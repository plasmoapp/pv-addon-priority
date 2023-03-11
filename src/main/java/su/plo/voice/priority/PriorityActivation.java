package su.plo.voice.priority;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.ProximityServerActivationHelper;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;

public final class PriorityActivation  {

    private final PlasmoVoiceServer voiceServer;

    private final PriorityAddon addon;
    private final PriorityConfig config;

    private ProximityServerActivationHelper proximityHelper;

    public PriorityActivation(@NotNull PlasmoVoiceServer voiceServer,
                              @NotNull PriorityAddon addon) {
        this.voiceServer = voiceServer;
        this.addon = addon;
        this.config = addon.getConfig();
    }

    public void register() {
        if (proximityHelper != null) {
            voiceServer.getEventBus().unregister(this, proximityHelper);
            voiceServer.getActivationManager().unregister(proximityHelper.getActivation());
            voiceServer.getSourceLineManager().unregister(proximityHelper.getSourceLine());
        }

        ServerActivation.Builder builder = voiceServer.getActivationManager().createBuilder(
                addon,
                "priority",
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
                .build();

        ServerSourceLine sourceLine = voiceServer.getSourceLineManager().createBuilder(
                addon,
                "priority",
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
        voiceServer.getEventBus().register(addon, proximityHelper);;
    }
}
