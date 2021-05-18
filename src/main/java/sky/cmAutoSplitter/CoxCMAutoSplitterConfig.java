package sky.cmAutoSplitter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autosplitter")
public interface CoxCMAutoSplitterConfig extends Config {

    @ConfigItem(position = 0, keyName = "splitIcePop", name = "Split on Ice demon pop-out", description = "Partial room split for Ice Demon")
    default boolean splitIcePop() {
        return false;
    }

    @ConfigItem(position = 1, keyName = "splitMuttadileTree", name = "Split on Muttadile tree cut", description = "Partial room split for Muttadiles")
    default boolean splitMuttadileTree() {
        return false;
    }

    @ConfigItem(position = 2, keyName = "port", name = "Port", description = "Port for the LiveSplit server. (Restart required)")
    default int port() {
        return 16834;
    }

    @ConfigItem(position = 3, keyName = "regular", name = "Split only on floors", description = "Split only on floor changes, useful for regular cox.")
    default boolean regular() {
        return false;
    }
}
