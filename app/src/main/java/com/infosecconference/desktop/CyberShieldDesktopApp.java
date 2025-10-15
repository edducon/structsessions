package com.infosecconference.desktop;

import com.infosecconference.desktop.config.AppConfiguration;
import com.infosecconference.desktop.config.BrandingTheme;
import com.infosecconference.desktop.db.DatabaseManager;
import com.infosecconference.desktop.service.DashboardService;
import com.infosecconference.desktop.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JOptionPane;

/**
 * Entry point for the CyberShield desktop management console.
 */
public final class CyberShieldDesktopApp {
    private CyberShieldDesktopApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                // We can continue with the default look and feel, but log to stdout for troubleshooting.
                System.err.println("Не удалось применить системную тему: " + e.getMessage());
            }

            try {
                AppConfiguration configuration = AppConfiguration.load();
                BrandingTheme theme = BrandingTheme.load();
                DatabaseManager databaseManager = DatabaseManager.from(configuration);

                DashboardService dashboardService = new DashboardService(databaseManager);

                MainWindow window = new MainWindow(configuration, theme, dashboardService);
                window.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Ошибка запуска приложения: " + ex.getMessage(),
                        "CyberShield Desktop",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
