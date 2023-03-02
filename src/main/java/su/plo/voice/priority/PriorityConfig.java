package su.plo.voice.priority;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import su.plo.config.Config;
import su.plo.config.ConfigField;
import su.plo.config.ConfigValidator;

import java.util.function.Predicate;

@Config
@Data
@Accessors(fluent = true)
public final class PriorityConfig {

    @ConfigField(path = "max_distance", comment = "Maximum priority distance")
    @ConfigValidator(value = DistanceValidator.class, allowed = "1-1024")
    private int maxDistance = 128;

    @ConfigField(path = "default_distance", comment = "Default priority distance")
    @ConfigValidator(value = DistanceValidator.class, allowed = "1-1024")
    private int defaultDistance = 48;

    @ConfigField(path = "activation_weight")
    private int activationWeight = 10;

    @ConfigField(path = "sourceline_weight")
    private int sourceLineWeight = 10;

    @ConfigField
    private boolean activationStereoSupport = false;

    @NoArgsConstructor
    public static class DistanceValidator implements Predicate<Object> {

        @Override
        public boolean test(Object o) {
            if (!(o instanceof Long)) return false;
            long port = (long) o;
            return port > 0 && port <= 1024;
        }
    }
}
