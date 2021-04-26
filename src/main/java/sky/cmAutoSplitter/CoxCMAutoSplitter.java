/*
 * Plugin for automating LiveSplits for Cox Challenge Mode.
 * Based on De0's CoxTimers.
 */

package sky.cmAutoSplitter;

import com.google.inject.Provides;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import javax.inject.Inject;

import static sky.cmAutoSplitter.CoxUtil.ICE_DEMON;
import static sky.cmAutoSplitter.CoxUtil.getroom_type;

@PluginDescriptor(name = "CM Auto splitter", description = "Auto splitter for live splits for cox cm")
public class CoxCMAutoSplitter extends Plugin {
    private NavigationButton navButton;
    private static final int RAID_STATE_VARBIT = 5425;
    private int prevRaidState = -1;

    @Inject
    private Client client;

    @Inject
    private CoxCMAutoSplitterConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    // LiveSplit server
    PrintWriter writer;

    // Room state
    private boolean in_raid;
    private final int[] cryp = new int[16];
    private final int[] cryx = new int[16];
    private final int[] cryy = new int[16];

    // Olm state
    private int olm_phase;

    // Misc state
    private boolean iceout, treecut;

    @Provides
    CoxCMAutoSplitterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(CoxCMAutoSplitterConfig.class);
    }

    @Subscribe
    public void onClientTick(ClientTick e) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return;

        if (clock() == 0 || !client.isInInstancedRegion()) {
            in_raid = false;
            return;
        }
        if (!in_raid) {
            in_raid = true;
            olm_phase = ~0;
            iceout = false;
            treecut = false;
        }
        for (int i = 0; i < 16; i++) {
            if (this.cryp[i] == -1)
                continue;
            int p = cryp[i];
            int x = cryx[i] - client.getBaseX();
            int y = cryy[i] - client.getBaseY();
            if (p != client.getPlane() || x < 0 || x >= 104 || y < 0 || y >= 104) {
                this.cryp[i] = -1;
                continue;
            }
            int flags = client.getCollisionMaps()[p].getFlags()[x][y];
            if ((flags & 0x100) == 0) {
                // combat and puzzle rooms
                send_split();
                this.cryp[i] = -1;
            }
        }
    }

    private static final String FL_COMPLETE_MES = "level complete! Duration: </col><col=ff0000>";

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        String mes = e.getMessage();
        if (e.getType() == ChatMessageType.FRIENDSCHATNOTIFICATION && mes.startsWith("<col=ef20ff>")) {
            int duration = mes.indexOf(FL_COMPLETE_MES);
            boolean is_fl_time = duration != -1;

            if (!is_fl_time)
                return;

            send_split();

        } else if (e.getType() == ChatMessageType.GAMEMESSAGE && mes.equals(
                "The Great Olm is giving its all. This is its final stand.")) {
            // head phase
            send_split();
            olm_phase = 99;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e) {
        GameObject go = e.getGameObject();
        switch (go.getId()) {
            case 29881: // Olm spawned
                if (olm_phase < 0) {
                    olm_phase = ~olm_phase;
                }
                break;
            case 30013:
                // Muttadile tree placeholder spawned after tree cut
                if (config.splitMuttadileTree() && !treecut) {
                    send_split();
                    treecut = true;
                }
                break;
            case 26209: // shamans/thieving/guardians
            case 29741: // mystics
            case 29749: // tightrope
            case 29753: // crabs
            case 29754:
            case 29755:
            case 29756:
            case 29757:
            case 29876: // ice
            case 30016: // vasa
            case 30017: // tekton/vanguards
            case 30018: // mutt
            case 30070: // vespula
                Point pt = go.getSceneMinLocation();
                int p = go.getPlane();
                int x = pt.getX();
                int y = pt.getY();
                int template = client.getInstanceTemplateChunks()[p][x / 8][y / 8];
                int roomtype = getroom_type(template);
                if (roomtype < 16) {
                    // add obstacle to list
                    cryp[roomtype] = p;
                    cryx[roomtype] = x + client.getBaseX();
                    cryy[roomtype] = y + client.getBaseY();
                }
                break;
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e) {
        if (e.getGameObject().getId() == ObjectID.LARGE_HOLE_29881) {
            send_split();
            olm_phase = ~olm_phase;
        }
    }

    private static final int SMOKE_PUFF = 188;

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e) {
        if (config.splitIcePop() && e.getGraphicsObject().getId() == SMOKE_PUFF && !iceout) {
            WorldPoint wp = WorldPoint.fromLocal(client, e.getGraphicsObject().getLocation());
            int p = client.getPlane();
            int x = wp.getX() - client.getBaseX();
            int y = wp.getY() - client.getBaseY();
            int template = client.getInstanceTemplateChunks()[p][x / 8][y / 8];
            if (CoxUtil.getroom_type(template) == ICE_DEMON) {
                send_split();
                iceout = true;
            }
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged e)
    {
        // when the raid starts
        int raidState = client.getVarbitValue(RAID_STATE_VARBIT);
        if (prevRaidState == 0 && raidState == 1){
            send_split();
        }
        prevRaidState = raidState;
    }

    private void send_split() {
        try {
            writer.write("startorsplit\r\n");
            writer.flush();
        } catch (Exception ignored) { }
    }

    private int clock() {
        return client.getVarbitValue(6386);
    }

    @Override
    protected void startUp() {
        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
        CoxCMAutoSplitterPanel panel = new CoxCMAutoSplitterPanel(client, writer, config, this);
        navButton = NavigationButton.builder().tooltip("LiveSplit controller")
                .icon(icon).priority(6).panel(panel).build();
        clientToolbar.addNavigation(navButton);

        panel.startPanel();
    }

    @Override
    protected void shutDown() {
        clientToolbar.removeNavigation(navButton);
    }
}
