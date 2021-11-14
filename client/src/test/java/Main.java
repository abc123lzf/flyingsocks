import com.lzf.flyingsocks.client.gui.ResourceManager;

import javax.imageio.ImageIO;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                }
                try {
                    final TrayIcon ti = new TrayIcon(ImageIO.read(ResourceManager.openSystemTrayImageStream()), "Have a nice day");
                    final JPopupMenu popup = new JPopupMenu();

                    JMenuItem mi = new JMenuItem("Get me some");
                    mi.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            SystemTray.getSystemTray().remove(ti);
                            System.exit(0);
                        }
                    });

                    popup.add(mi);
                    ti.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1) {
                                Rectangle bounds = getSafeScreenBounds(e.getPoint());
                                Point point = e.getPoint();
                                int x = point.x;
                                int y = point.y;
                                if (y < bounds.y) {
                                    y = bounds.y;
                                } else if (y > bounds.y + bounds.height) {
                                    y = bounds.y + bounds.height;
                                }
                                if (x < bounds.x) {
                                    x = bounds.x;
                                } else if (x > bounds.x + bounds.width) {
                                    x = bounds.x + bounds.width;
                                }
                                if (x + popup.getPreferredSize().width > bounds.x + bounds.width) {
                                    x = (bounds.x + bounds.width) - popup.getPreferredSize().width;
                                }
                                if (y + popup.getPreferredSize().height > bounds.y + bounds.height) {
                                    y = (bounds.y + bounds.height) - popup.getPreferredSize().height;
                                }
                                popup.setLocation(x, y);
                                popup.setVisible(true);
                            }
                        }
                    });

                    SystemTray.getSystemTray().add(ti);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static Rectangle getSafeScreenBounds(Point pos) {

        Rectangle bounds = getScreenBoundsAt(pos);
        Insets insets = getScreenInsetsAt(pos);

        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);

        return bounds;

    }

    public static Insets getScreenInsetsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Insets insets = null;
        if (gd != null) {
            insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
        }
        return insets;
    }

    public static Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Rectangle bounds = null;
        if (gd != null) {
            bounds = gd.getDefaultConfiguration().getBounds();
        }
        return bounds;
    }

    public static GraphicsDevice getGraphicsDeviceAt(Point pos) {

        GraphicsDevice device = null;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice lstGDs[] = ge.getScreenDevices();

        ArrayList<GraphicsDevice> lstDevices = new ArrayList<GraphicsDevice>(lstGDs.length);

        for (GraphicsDevice gd : lstGDs) {

            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {

                lstDevices.add(gd);

            }

        }

        if (lstDevices.size() > 0) {
            device = lstDevices.get(0);
        } else {
            device = ge.getDefaultScreenDevice();
        }

        return device;

    }
}