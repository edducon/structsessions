package com.infosecconference.desktop.ui;

import com.infosecconference.desktop.config.AppConfiguration;
import com.infosecconference.desktop.config.BrandingTheme;
import com.infosecconference.desktop.model.Activity;
import com.infosecconference.desktop.model.ConferenceUser;
import com.infosecconference.desktop.model.Event;
import com.infosecconference.desktop.model.SummarySnapshot;
import com.infosecconference.desktop.service.DashboardService;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainWindow extends JFrame {
    private final AppConfiguration configuration;
    private final BrandingTheme theme;
    private final DashboardService dashboardService;

    private final SummaryPanel summaryPanel;
    private final EventsPanel eventsPanel;
    private final PeoplePanel peoplePanel;
    private final SchedulePanel schedulePanel;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMMM HH:mm", new Locale("ru"));

    public MainWindow(AppConfiguration configuration,
                      BrandingTheme theme,
                      DashboardService dashboardService) {
        super("CyberShield Desktop");
        this.configuration = configuration;
        this.theme = theme;
        this.dashboardService = dashboardService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setLocationRelativeTo(null);

        summaryPanel = new SummaryPanel(theme);
        eventsPanel = new EventsPanel(theme);
        peoplePanel = new PeoplePanel(theme);
        schedulePanel = new SchedulePanel(theme);

        buildLayout();
        refreshData();
    }

    private void buildLayout() {
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(theme.baseFont());

        JPanel dashboardTab = new JPanel(new BorderLayout());
        dashboardTab.add(summaryPanel, BorderLayout.NORTH);
        dashboardTab.add(eventsPanel, BorderLayout.CENTER);

        tabs.addTab("Панель", dashboardTab);
        tabs.addTab("Команда", peoplePanel);
        tabs.addTab("Расписание", schedulePanel);

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(theme.primaryColor());
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel logoLabel = new JLabel(loadLogoIcon());
        logoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        header.add(logoLabel, BorderLayout.WEST);

        JLabel title = new JLabel("CyberShield — управление конференцией");
        title.setForeground(Color.WHITE);
        title.setFont(theme.titleFont());
        header.add(title, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);

        JButton dataButton = new JButton("Открыть таблицы и данные");
        dataButton.setFont(theme.baseFont());
        dataButton.addActionListener(e -> openMaterialsFolder());
        actions.add(dataButton);

        JButton styleButton = new JButton("Открыть руководство по стилю");
        styleButton.setFont(theme.baseFont());
        styleButton.addActionListener(e -> openStyleGuide());
        actions.add(styleButton);

        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private ImageIcon loadLogoIcon() {
        Path logoPath = configuration.imageRoot().resolve("logo.png");
        if (Files.exists(logoPath)) {
            try {
                Image image = new ImageIcon(logoPath.toString()).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                return new ImageIcon(image);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return new ImageIcon(new byte[0]);
    }

    private void openStyleGuide() {
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this, "Операция не поддерживается в данной среде", "Style Guide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().open(configuration.styleGuidePath().toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось открыть руководство: " + ex.getMessage(),
                    "Style Guide",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openMaterialsFolder() {
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(this,
                    "Операция не поддерживается в данной среде",
                    "Материалы",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path materials = configuration.materialsRoot();
        if (!Files.exists(materials)) {
            JOptionPane.showMessageDialog(this,
                    "Папка с материалами пока отсутствует: " + materials,
                    "Материалы",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Desktop.getDesktop().open(materials.toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Не удалось открыть каталог: " + ex.getMessage(),
                    "Материалы",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshData() {
        new SwingWorker<Void, Void>() {
            private SummarySnapshot summary;
            private List<Event> events;
            private Map<String, List<ConferenceUser>> users;
            private List<Activity> activities;

            @Override
            protected Void doInBackground() {
                try {
                    summary = dashboardService.loadSummary();
                    events = dashboardService.loadEvents();
                    activities = dashboardService.loadUpcomingActivities(12);
                    users = new HashMap<>();
                    users.put("PARTICIPANT", dashboardService.loadUsersByRole("PARTICIPANT"));
                    users.put("MODERATOR", dashboardService.loadUsersByRole("MODERATOR"));
                    users.put("JURY", dashboardService.loadUsersByRole("JURY"));
                    users.put("ORGANIZER", dashboardService.loadUsersByRole("ORGANIZER"));
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this,
                            "Не удалось загрузить данные: " + ex.getMessage(),
                            "База данных",
                            JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void done() {
                if (summary != null) {
                    summaryPanel.update(summary);
                }
                if (events != null) {
                    eventsPanel.update(events);
                }
                if (activities != null) {
                    schedulePanel.update(activities);
                }
                if (users != null) {
                    peoplePanel.update(users);
                }
            }
        }.execute();
    }

    private static class SummaryPanel extends JPanel {
        private final JLabel eventsLabel = createCardLabel();
        private final JLabel activitiesLabel = createCardLabel();
        private final JLabel participantsLabel = createCardLabel();
        private final JLabel moderatorsLabel = createCardLabel();
        private final JLabel juryLabel = createCardLabel();
        private final JLabel organizersLabel = createCardLabel();
        private final JLabel teamsLabel = createCardLabel();

        private SummaryPanel(BrandingTheme theme) {
            setLayout(new GridLayout(1, 7, 12, 12));
            setBorder(new EmptyBorder(16, 16, 16, 16));
            setBackground(theme.surfaceColor());

            add(buildCard("Мероприятия", eventsLabel, theme));
            add(buildCard("Активности", activitiesLabel, theme));
            add(buildCard("Участники", participantsLabel, theme));
            add(buildCard("Модераторы", moderatorsLabel, theme));
            add(buildCard("Жюри", juryLabel, theme));
            add(buildCard("Организаторы", organizersLabel, theme));
            add(buildCard("Команды", teamsLabel, theme));
        }

        private static JLabel createCardLabel() {
            JLabel label = new JLabel("0", SwingConstants.CENTER);
            label.setFont(new Font("Comic Sans MS", Font.BOLD, 28));
            label.setForeground(Color.WHITE);
            return label;
        }

        private JPanel buildCard(String title, JLabel valueLabel, BrandingTheme theme) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(theme.secondaryColor());
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    BorderFactory.createLineBorder(theme.accentColor(), 2, true)));

            JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setForeground(theme.accentColor());
            titleLabel.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));

            panel.add(titleLabel, BorderLayout.NORTH);
            panel.add(valueLabel, BorderLayout.CENTER);
            return panel;
        }

        private void update(SummarySnapshot snapshot) {
            eventsLabel.setText(String.valueOf(snapshot.events()));
            activitiesLabel.setText(String.valueOf(snapshot.activities()));
            participantsLabel.setText(String.valueOf(snapshot.participants()));
            moderatorsLabel.setText(String.valueOf(snapshot.moderators()));
            juryLabel.setText(String.valueOf(snapshot.jury()));
            organizersLabel.setText(String.valueOf(snapshot.organizers()));
            teamsLabel.setText(String.valueOf(snapshot.teams()));
        }
    }

    private static class EventsPanel extends JPanel {
        private final DefaultTableModel model;

        private EventsPanel(BrandingTheme theme) {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(16, 16, 16, 16));
            setBackground(theme.surfaceColor());

            model = new DefaultTableModel(new Object[]{"Мероприятие", "Даты", "Площадка"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            JTable table = new JTable(model);
            table.setRowHeight(28);
            table.setFont(theme.baseFont());
            table.getTableHeader().setFont(theme.baseFont().deriveFont(Font.BOLD));
            add(new JScrollPane(table), BorderLayout.CENTER);
        }

        private void update(List<Event> events) {
            model.setRowCount(0);
            for (Event event : events) {
                String dateRange = buildDateRange(event.startDate(), event.endDate());
                String venue = event.city() != null ? event.city().name() : event.venue();
                model.addRow(new Object[]{event.title(), dateRange, venue});
            }
        }

        private String buildDateRange(java.time.LocalDate start, java.time.LocalDate end) {
            if (start == null) {
                return "Не указано";
            }
            if (end == null || end.equals(start)) {
                return DATE_FORMATTER.format(start);
            }
            return DATE_FORMATTER.format(start) + " — " + DATE_FORMATTER.format(end);
        }
    }

    private static class PeoplePanel extends JPanel {
        private final Map<String, DefaultTableModel> models = new HashMap<>();
        private final JTabbedPane tabs;

        private PeoplePanel(BrandingTheme theme) {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(16, 16, 16, 16));

            tabs = new JTabbedPane();
            tabs.setFont(theme.baseFont());

            addTab(theme, "Участники", "PARTICIPANT");
            addTab(theme, "Модераторы", "MODERATOR");
            addTab(theme, "Жюри", "JURY");
            addTab(theme, "Организаторы", "ORGANIZER");

            add(tabs, BorderLayout.CENTER);
        }

        private void addTab(BrandingTheme theme, String title, String key) {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"ФИО", "Email", "Телефон", "Организация"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            models.put(key, model);
            JTable table = new JTable(model);
            table.setRowHeight(26);
            table.setFont(theme.baseFont());
            table.getTableHeader().setFont(theme.baseFont().deriveFont(Font.BOLD));
            tabs.addTab(title, new JScrollPane(table));
        }

        private void update(Map<String, List<ConferenceUser>> users) {
            for (Map.Entry<String, DefaultTableModel> entry : models.entrySet()) {
                entry.getValue().setRowCount(0);
                List<ConferenceUser> list = users.getOrDefault(entry.getKey(), List.of());
                for (ConferenceUser user : list) {
                    entry.getValue().addRow(new Object[]{user.fullName(), user.email(), user.phone(), user.organization()});
                }
            }
        }
    }

    private static class SchedulePanel extends JPanel {
        private final DefaultTableModel model;

        private SchedulePanel(BrandingTheme theme) {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(16, 16, 16, 16));

            model = new DefaultTableModel(new Object[]{"Активность", "Мероприятие", "Начало", "Модератор", "Победитель"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            JTable table = new JTable(model);
            table.setRowHeight(26);
            table.setFont(theme.baseFont());
            table.getTableHeader().setFont(theme.baseFont().deriveFont(Font.BOLD));
            add(new JScrollPane(table), BorderLayout.CENTER);
        }

        private void update(List<Activity> activities) {
            model.setRowCount(0);
            for (Activity activity : activities) {
                String start = activity.startTime() != null ? DATE_TIME_FORMATTER.format(activity.startTime()) : "Не задано";
                String moderator = activity.moderator() != null ? activity.moderator().fullName() : "-";
                model.addRow(new Object[]{
                        activity.name(),
                        activity.event() != null ? activity.event().title() : "-",
                        start,
                        moderator,
                        activity.winnerTeam() != null ? activity.winnerTeam() : "-"
                });
            }
        }
    }
}
