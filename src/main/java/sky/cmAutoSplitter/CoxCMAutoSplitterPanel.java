package sky.cmAutoSplitter;

import java.awt.*;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.LineBorder;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import java.io.PrintWriter;
import java.net.Socket;

public class CoxCMAutoSplitterPanel extends PluginPanel
{
    private final Client client;
    private final CoxCMAutoSplitterConfig config;
    private final CoxCMAutoSplitter splitter;
    private PrintWriter writer;
    private Socket socket;
    private JLabel status;

    @Inject
    CoxCMAutoSplitterPanel(Client client, PrintWriter writer, CoxCMAutoSplitterConfig config, CoxCMAutoSplitter splitter){
        this.client = client;
        this.writer = writer;
        this.config = config;
        this.splitter = splitter;
    }

    // TODO Make buttons not have the wierd outline when clicked
    // TODO Make the icon have transparent border

    private void connect(){
        try {
            socket = new Socket("localhost", config.port());
            writer = new PrintWriter(socket.getOutputStream());
            splitter.writer = writer;

            set_connected();

            if (client.getGameState() == GameState.LOGGED_IN) {
                String message = "Socket started at port <col=ff0000>" + config.port() + "</col>.";
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            }

        } catch (Exception e) {
            String message = "Could not start socket, did you start the LiveSplit server?";
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
        }
    }

    private void disconnect(){
        try {
            socket.close();
            set_disconnected();

            if (client.getGameState() == GameState.LOGGED_IN) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Socket closed.", null);
            }
        } catch (Exception ignored) {}
    }

    private void control(String cmd){
        writer.write(cmd + "\r\n");
        writer.flush();
    }

    public void startPanel(){
        getParent().setLayout(new BorderLayout());
        getParent().add(this, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        final JPanel layout = new JPanel();
        BoxLayout boxLayout = new BoxLayout(layout, BoxLayout.Y_AXIS);
        layout.setLayout(boxLayout);
        add(layout, BorderLayout.NORTH);

        JPanel statusFrame = new JPanel();
        statusFrame.setLayout(new GridBagLayout());
        statusFrame.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN), "Status"));

        status = new JLabel("Not connected");
        status.setForeground(Color.RED);
        statusFrame.add(status);


        JPanel connectionFrame = new JPanel();
        connectionFrame.setLayout(new GridLayout(2, 1));
        connectionFrame.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN), "Connection"));

        JButton b_connect = new JButton("Connect");
        JButton b_disconnect = new JButton("Disconnect");

        b_connect.addActionListener(e -> connect());
        b_disconnect.addActionListener(e -> disconnect());

        connectionFrame.add(b_connect);
        connectionFrame.add(b_disconnect);


        JPanel controllerFrame = new JPanel();
        controllerFrame.setLayout(new GridLayout(6, 1));
        controllerFrame.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN), "LiveSplit Controller"));

        JButton b_split = new JButton("Split");
        JButton b_reset = new JButton("Reset");
        JButton b_undo = new JButton("Undo split");
        JButton b_skip = new JButton("Skip split");
        JButton b_pause = new JButton("Pause");
        JButton b_resume = new JButton("Resume");

        b_split.addActionListener(e -> control("startorsplit"));
        b_reset.addActionListener(e -> control("reset"));
        b_undo.addActionListener(e -> control("unsplit"));
        b_skip.addActionListener(e -> control("skipsplit"));
        b_pause.addActionListener(e -> control("pause"));
        b_resume.addActionListener(e -> control("resume"));

        controllerFrame.add(b_split, BorderLayout.CENTER);
        controllerFrame.add(b_reset, BorderLayout.CENTER);
        controllerFrame.add(b_undo, BorderLayout.CENTER);
        controllerFrame.add(b_skip, BorderLayout.CENTER);
        controllerFrame.add(b_pause, BorderLayout.CENTER);
        controllerFrame.add(b_resume, BorderLayout.CENTER);

        layout.add(statusFrame);
        layout.add(Box.createRigidArea(new Dimension(0, 15)));
        layout.add(connectionFrame);
        layout.add(Box.createRigidArea(new Dimension(0, 15)));
        layout.add(controllerFrame);
    }

    public void set_connected(){
        status.setText("Connected");
        status.setForeground(Color.GREEN);
    }

    public void set_disconnected(){
        status.setText("Not connected");
        status.setForeground(Color.RED);
    }
}
