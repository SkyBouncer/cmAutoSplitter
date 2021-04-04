package sky.cmAutoSplitter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ModifierlessKeybind;

import java.awt.event.KeyEvent;

@ConfigGroup("autosplitter")
public interface CoxCMAutoSplitterConfig extends Config {

  @ConfigItem(position = 0, keyName = "splitIcePop", name = "Split on Ice demon pop-out", description = "Partial room split for Ice Demon")
  default boolean splitIcePop() {
    return true;
  }

  @ConfigItem(position = 1, keyName = "splitMuttadileTree", name = "Split on Muttadile tree cut", description = "Partial room split for Muttadiles")
  default boolean splitMuttadileTree() {
    return true;
  }

  @ConfigItem(position = 2, keyName = "key", name = "Key for LiveSplit", description = "The key you want LiveSplit to split on.")
  default ModifierlessKeybind key()
  {
    return new ModifierlessKeybind(KeyEvent.VK_NUMPAD0, 0);
  }

}
