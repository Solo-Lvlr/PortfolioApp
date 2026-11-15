import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// =====================================================
// MAIN APPLICATION
// =====================================================

public class PortfolioApp extends JFrame {


    private final Portfolio portfolio = new Portfolio();
    private final MarketSimulator market = new MarketSimulator(portfolio);

    private final ExecutorService executor =
            Executors.newFixedThreadPool(3);

    private DefaultTableModel tableModel;
    private JTable assetTable;

    private JLabel totalValueLabel;
    private JLabel investmentLabel;
    private JLabel profitLabel;
    private JLabel statusLabel;

    private JTextArea dashboardOverview;
    private JTextArea reportArea;

    private JComboBox<RiskProfile> riskSelector;

    // =================================================
    // CONSTRUCTOR
    // =================================================

    public PortfolioApp() {

        setTitle("Autonomous Stock Portfolio Balancing Engine");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initial sample assets
        portfolio.addAsset(
                new StockAsset("Apple Inc", "AAPL", 10, 180));

        portfolio.addAsset(
                new StockAsset("Microsoft", "MSFT", 8, 400));

        portfolio.addAsset(
                new BondAsset("Government Bond", "GB01", 15, 100));

        portfolio.addAsset(
                new CommodityAsset("Gold", "GOLD", 5, 220));

        createGUI();

        // IMPORTANT: Connect market updates to GUI refresh
        market.setOnUpdate(this::refreshDashboard);

        refreshDashboard();

        // Start automatic market simulation
        market.start();

        addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {

                market.stop();
                executor.shutdownNow();
            }
        });
    }

    // =================================================
    // GUI CREATION
    // =================================================

    private void createGUI() {

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboard());
        tabs.addTab("Assets", createAssetsPanel());
        tabs.addTab("Market Simulator", createMarketPanel());
        tabs.addTab("Portfolio Balancer", createBalancerPanel());
        tabs.addTab("Reports", createReportsPanel());

        add(tabs, BorderLayout.CENTER);

        statusLabel = new JLabel("System Ready");
        statusLabel.setBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12));

        add(statusLabel, BorderLayout.SOUTH);
    }

    // =================================================
    // DASHBOARD
    // =================================================

    private JPanel createDashboard() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));

        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel cards = new JPanel(
                new GridLayout(1, 3, 15, 15));

        totalValueLabel = new JLabel();
        investmentLabel = new JLabel();
        profitLabel = new JLabel();

        cards.add(createCard("Portfolio Value", totalValueLabel));
        cards.add(createCard("Total Investment", investmentLabel));
        cards.add(createCard("Profit / Loss", profitLabel));

        JLabel heading = new JLabel("Portfolio Overview");

        heading.setFont(
                new Font("Arial", Font.BOLD, 22));

        dashboardOverview = new JTextArea();

        dashboardOverview.setEditable(false);
        dashboardOverview.setFont(
                new Font("Monospaced", Font.PLAIN, 15));

        JPanel overviewPanel = new JPanel(new BorderLayout());

        overviewPanel.add(heading, BorderLayout.NORTH);
        overviewPanel.add(
                new JScrollPane(dashboardOverview),
                BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh Dashboard");

        refresh.addActionListener(e -> refreshDashboard());

        overviewPanel.add(refresh, BorderLayout.SOUTH);

        panel.add(cards, BorderLayout.NORTH);
        panel.add(overviewPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCard(String title, JLabel value) {

        JPanel card = new JPanel(new GridLayout(2, 1, 5, 5));

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(
                                15, 15, 15, 15)));

        JLabel heading = new JLabel(title);

        heading.setFont(
                new Font("Arial", Font.BOLD, 15));

        value.setFont(
                new Font("Arial", Font.BOLD, 22));

        card.add(heading);
        card.add(value);

        return card;
    }

    // =================================================
    // ASSET MANAGEMENT
    // =================================================

    private JPanel createAssetsPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {
                "Type",
                "Name",
                "Symbol",
                "Quantity",
                "Price",
                "Total Purchased Price",
                "New Price",
                "Market Value"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        assetTable = new JTable(tableModel);

        JButton addButton = new JButton("Add Asset");
        JButton removeButton = new JButton("Remove Asset");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> addAssetDialog());

        removeButton.addActionListener(
                e -> removeSelectedAsset());

        refreshButton.addActionListener(
                e -> refreshDashboard());

        JPanel buttons = new JPanel();

        buttons.add(addButton);
        buttons.add(removeButton);
        buttons.add(refreshButton);

        panel.add(
                new JScrollPane(assetTable),
                BorderLayout.CENTER);

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private void addAssetDialog() {

        JTextField name = new JTextField();
        JTextField symbol = new JTextField();
        JTextField quantity = new JTextField();
        JTextField price = new JTextField();

        String[] types = {
                "Stock", "Bond", "Commodity"
        };

        JComboBox<String> typeBox =
                new JComboBox<>(types);

        Object[] fields = {
                "Asset Type:", typeBox,
                "Asset Name:", name,
                "Symbol:", symbol,
                "Quantity:", quantity,
                "Price:", price
        };

        int result = JOptionPane.showConfirmDialog(
                this,
                fields,
                "Add New Asset",
                JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {

            String n = name.getText().trim();
            String s = symbol.getText().trim();

            double q = Double.parseDouble(quantity.getText());
            double p = Double.parseDouble(price.getText());

            if (n.isEmpty() || s.isEmpty() || q <= 0 || p <= 0) {

                throw new IllegalArgumentException(
                        "Enter valid asset details.");
            }

            Asset asset;

            switch ((String) typeBox.getSelectedItem()) {

                case "Stock":
                    asset = new StockAsset(n, s, q, p);
                    break;

                case "Bond":
                    asset = new BondAsset(n, s, q, p);
                    break;

                default:
                    asset = new CommodityAsset(n, s, q, p);
            }

            portfolio.addAsset(asset);

            refreshDashboard();

            statusLabel.setText("Asset added successfully.");

        } catch (NumberFormatException ex) {

            showError("Quantity and price must be numbers.");

        } catch (IllegalArgumentException ex) {

            showError(ex.getMessage());
        }
    }

    private void removeSelectedAsset() {

        int row = assetTable.getSelectedRow();

        if (row == -1) {

            showError("Select an asset first.");
            return;
        }

        portfolio.removeAsset(row);

        refreshDashboard();

        statusLabel.setText("Asset removed.");
    }

    // =================================================
    // MARKET SIMULATOR GUI
    // =================================================

    private JPanel createMarketPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));

        JLabel heading = new JLabel(
                "SIMULATED MARKET PRICE MONITOR");

        heading.setFont(
                new Font("Arial", Font.BOLD, 22));

        JTextArea info = new JTextArea(
                "MARKET SIMULATION\n\n" +
                "Market simulation runs in background threads.\n\n" +
                "Asset prices change automatically every 3 seconds.\n\n" +
                "The main GUI remains responsive.\n\n" +
                "No real stock market data is used.");

        info.setEditable(false);

        info.setFont(
                new Font("Arial", Font.PLAIN, 16));

        JButton simulate = new JButton(
                "Simulate Market Change");

        JButton start = new JButton(
                "Start Automatic Simulation");

        JButton stop = new JButton(
                "Stop Simulation");

        simulate.addActionListener(e -> {

            executor.submit(() -> market.simulateOnce());

            statusLabel.setText(
                    "Manual market simulation requested.");
        });

        start.addActionListener(e -> {

            market.start();

            statusLabel.setText(
                    "Market simulation started.");
        });

        stop.addActionListener(e -> {

            market.stop();

            statusLabel.setText(
                    "Market simulation stopped.");
        });

        JPanel buttons = new JPanel();

        buttons.add(simulate);
        buttons.add(start);
        buttons.add(stop);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(new JScrollPane(info), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    // =================================================
    // PORTFOLIO BALANCER GUI
    // =================================================

    private JPanel createBalancerPanel() {

        JPanel panel = new JPanel(new BorderLayout(15, 15));

        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel(
                "AUTONOMOUS PORTFOLIO BALANCER");

        heading.setFont(
                new Font("Arial", Font.BOLD, 22));

        riskSelector =
                new JComboBox<>(RiskProfile.values());

        JButton balanceButton =
                new JButton("Balance Portfolio");

        JTextArea result = new JTextArea(15, 50);

        result.setEditable(false);

        result.setFont(
                new Font("Monospaced", Font.PLAIN, 14));

        balanceButton.addActionListener(e -> {

            RiskProfile selected =
                    (RiskProfile) riskSelector.getSelectedItem();

            try {

                PortfolioBalancer<Asset> balancer =
                        new PortfolioBalancer<>(portfolio);

                String output = balancer.balance(selected);

                result.setText(output);

                statusLabel.setText(
                        "Portfolio balancing completed.");

            } catch (MarketClosedException |
                     OverexposureLimitException ex) {

                showError(ex.getMessage());
            }
        });

        JPanel controls = new JPanel();

        controls.add(new JLabel("Risk Profile:"));
        controls.add(riskSelector);
        controls.add(balanceButton);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(controls, BorderLayout.CENTER);
        panel.add(
                new JScrollPane(result),
                BorderLayout.SOUTH);

        return panel;
    }

    // =================================================
    // REPORTS GUI
    // =================================================

    private JPanel createReportsPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        reportArea = new JTextArea();

        reportArea.setEditable(false);

        reportArea.setFont(
                new Font("Monospaced", Font.PLAIN, 14));

        JButton generate =
                new JButton("Generate Report");

        JButton save =
                new JButton("Save Portfolio");

        JButton load =
                new JButton("Load Portfolio");

        generate.addActionListener(e ->
                reportArea.setText(generateReport()));

        save.addActionListener(e -> savePortfolio());

        load.addActionListener(e -> loadPortfolio());

        JPanel buttons = new JPanel();

        buttons.add(generate);
        buttons.add(save);
        buttons.add(load);

        panel.add(
                new JScrollPane(reportArea),
                BorderLayout.CENTER);

        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    // =================================================
    // DASHBOARD REFRESH
    // =================================================

    private void refreshDashboard() {

        // Ensure all Swing changes happen on EDT
        if (!SwingUtilities.isEventDispatchThread()) {

            SwingUtilities.invokeLater(this::refreshDashboard);
            return;
        }

        double total = portfolio.getTotalValue();

        double invested =
                portfolio.getTotalInvestment();

        double profit = total - invested;

        totalValueLabel.setText(format(total));

        investmentLabel.setText(format(invested));

        profitLabel.setText(
                (profit >= 0 ? "+" : "") + format(profit));

        // Refresh asset table
        tableModel.setRowCount(0);

        for (Asset asset : portfolio.getAssets()) {

            tableModel.addRow(new Object[] {

                    asset.getType(),
                    asset.getName(),
                    asset.getSymbol(),
                    asset.getQuantity(),
                    format(asset.getPurchasePrice()),
                    format(asset.getTotalPurchasePrice()),
                    format(asset.getCurrentPrice()),
                    format(asset.getCurrentValue())
            });
        }

        // Refresh dashboard overview
        StringBuilder text = new StringBuilder();

        text.append("TOTAL ASSETS: ")
                .append(portfolio.getAssets().size())
                .append("\n\n");

        for (Asset asset : portfolio.getAssets()) {

            text.append(asset.getName())
                    .append(" (")
                    .append(asset.getType())
                    .append(") : ")
                    .append(format(asset.getCurrentValue()))
                    .append("\n");
        }

        text.append("\nTOTAL PORTFOLIO VALUE: ")
                .append(format(total));

        text.append("\nTOTAL PROFIT / LOSS: ")
                .append(format(profit));

        dashboardOverview.setText(text.toString());
    }

    // =================================================
    // REPORT GENERATION
    // =================================================

    private String generateReport() {

        StringBuilder report = new StringBuilder();

        report.append("AUTONOMOUS PORTFOLIO REPORT\n");

        report.append(
                "================================\n\n");

        report.append("Total Assets: ")
                .append(portfolio.getAssets().size())
                .append("\n");

        report.append("Total Investment: ")
                .append(format(portfolio.getTotalInvestment()))
                .append("\n");

        report.append("Current Value: ")
                .append(format(portfolio.getTotalValue()))
                .append("\n");

        report.append("Profit/Loss: ")
                .append(format(
                        portfolio.getTotalValue()
                                - portfolio.getTotalInvestment()))
                .append("\n\n");

        report.append("ASSET DETAILS\n");

        report.append(
                "--------------------------------\n");

        for (Asset asset : portfolio.getAssets()) {

            report.append(asset.getName())
                    .append(" | ")
                    .append(asset.getType())
                    .append(" | ")
                    .append(format(asset.getCurrentValue()))
                    .append("\n");
        }

        report.append("\nRISK DISTRIBUTION\n");

        Map<String, Double> distribution =
                portfolio.getDistribution();

        distribution.forEach((type, value) -> {

            report.append(type)
                    .append(" : ")
                    .append(String.format("%.2f%%", value))
                    .append("\n");
        });

        return report.toString();
    }

    // =================================================
    // FILE HANDLING - SAVE
    // =================================================

    private void savePortfolio() {

        try (PrintWriter writer =
                     new PrintWriter(
                             new FileWriter("portfolio.txt"))) {

            for (Asset asset : portfolio.getAssets()) {

                writer.println(
                        asset.getType() + "|" +
                        asset.getName() + "|" +
                        asset.getSymbol() + "|" +
                        asset.getQuantity() + "|" +
                        asset.getCurrentPrice() + "|" +
                        asset.getPurchasePrice());
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Portfolio saved to portfolio.txt");

        } catch (IOException ex) {

            showError(
                    "Unable to save portfolio: " + ex.getMessage());
        }
    }

    // =================================================
    // FILE HANDLING - LOAD
    // =================================================

    private void loadPortfolio() {

        File file = new File("portfolio.txt");

        if (!file.exists()) {

            showError("portfolio.txt does not exist.");
            return;
        }

        try (BufferedReader reader =
                     new BufferedReader(
                             new FileReader(file))) {

            List<Asset> loadedAssets = new ArrayList<>();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] data = line.split("\\|", -1);

                if (data.length != 6) continue;

                String type = data[0];
                String name = data[1];
                String symbol = data[2];

                double quantity =
                        Double.parseDouble(data[3]);

                double currentPrice =
                        Double.parseDouble(data[4]);

                double purchasePrice =
                        Double.parseDouble(data[5]);

                Asset asset;

                switch (type) {

                    case "Stock":
                        asset = new StockAsset(
                                name, symbol,
                                quantity, purchasePrice);
                        break;

                    case "Bond":
                        asset = new BondAsset(
                                name, symbol,
                                quantity, purchasePrice);
                        break;

                    case "Commodity":
                        asset = new CommodityAsset(
                                name, symbol,
                                quantity, purchasePrice);
                        break;

                    default:
                        continue;
                }

                asset.setCurrentPrice(currentPrice);

                loadedAssets.add(asset);
            }

            portfolio.replaceAssets(loadedAssets);

            refreshDashboard();

            JOptionPane.showMessageDialog(
                    this,
                    "Portfolio loaded successfully.");

        } catch (IOException |
                 IllegalArgumentException ex) {

            showError(
                    "Unable to load portfolio: " + ex.getMessage());
        }
    }

    // =================================================
    // UTILITY METHODS
    // =================================================

    private String format(double value) {

        return String.format("₹%,.2f", value);
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    // =================================================
    // MAIN METHOD
    // =================================================

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());

            } catch (Exception ignored) {}

            new PortfolioApp().setVisible(true);
        });
    }

    // =================================================
    // INTERFACE
    // =================================================

    interface Valuable {

        double getCurrentValue();

        double getCurrentPrice();
    }

    // =================================================
    // ENUM
    // =================================================

    enum RiskProfile {

        CONSERVATIVE,
        MODERATE,
        AGGRESSIVE
    }

    // =================================================
    // ABSTRACT ASSET CLASS
    // =================================================

    abstract static class Asset implements Valuable {

        private final String name;
        private final String symbol;

        private double quantity;
        private final double purchasePrice;

        // volatile ensures visibility of background price updates
        private volatile double currentPrice;

        public Asset(
                String name,
                String symbol,
                double quantity,
                double price) {

            if (name == null || name.trim().isEmpty()
                    || symbol == null || symbol.trim().isEmpty()
                    || quantity <= 0 || price <= 0
                    || !Double.isFinite(quantity)
                    || !Double.isFinite(price)) {

                throw new IllegalArgumentException(
                        "Invalid asset information.");
            }

            this.name = name;
            this.symbol = symbol;
            this.quantity = quantity;
            this.purchasePrice = price;
            this.currentPrice = price;
        }

        public abstract String getType();

        @Override
        public double getCurrentValue() {

            return quantity * currentPrice;
        }

        @Override
        public double getCurrentPrice() {

            return currentPrice;
        }

        public double getQuantity() {

            return quantity;
        }

        public double getPurchasePrice() {

            return purchasePrice;
        }

        public double getTotalPurchasePrice() {

            return quantity * purchasePrice;
        }

        public String getName() {

            return name;
        }

        public String getSymbol() {

            return symbol;
        }

        public void setCurrentPrice(double price) {

            if (price <= 0 || !Double.isFinite(price)) {

                throw new IllegalArgumentException(
                        "Price must be positive and finite.");
            }

            currentPrice = price;
        }

        public void setQuantity(double quantity) {

            if (quantity < 0 || !Double.isFinite(quantity)) {

                throw new IllegalArgumentException(
                        "Invalid quantity.");
            }

            this.quantity = quantity;
        }
    }

    // =================================================
    // INHERITANCE
    // =================================================

    static class StockAsset extends Asset {

        public StockAsset(
                String name,
                String symbol,
                double quantity,
                double price) {

            super(name, symbol, quantity, price);
        }

        @Override
        public String getType() {

            return "Stock";
        }
    }

    static class BondAsset extends Asset {

        public BondAsset(
                String name,
                String symbol,
                double quantity,
                double price) {

            super(name, symbol, quantity, price);
        }

        @Override
        public String getType() {

            return "Bond";
        }
    }

    static class CommodityAsset extends Asset {

        public CommodityAsset(
                String name,
                String symbol,
                double quantity,
                double price) {

            super(name, symbol, quantity, price);
        }

        @Override
        public String getType() {

            return "Commodity";
        }
    }

    // =================================================
    // PORTFOLIO CLASS
    // =================================================

    static class Portfolio {

        private final List<Asset> assets =
                Collections.synchronizedList(
                        new ArrayList<>());

        public void addAsset(Asset asset) {

            synchronized (assets) {

                assets.add(asset);
            }
        }

        public void removeAsset(int index) {

            synchronized (assets) {

                if (index >= 0 && index < assets.size()) {

                    assets.remove(index);
                }
            }
        }

        public List<Asset> getAssets() {

            synchronized (assets) {

                return new ArrayList<>(assets);
            }
        }

        public void replaceAssets(List<Asset> newAssets) {

            synchronized (assets) {

                assets.clear();

                assets.addAll(newAssets);
            }
        }

        public double getTotalValue() {

            return getAssets().stream()
                    .mapToDouble(Asset::getCurrentValue)
                    .sum();
        }

        public double getTotalInvestment() {

            return getAssets().stream()
                    .mapToDouble(asset ->
                            asset.getQuantity()
                                    * asset.getPurchasePrice())
                    .sum();
        }

        public Map<String, Double> getDistribution() {

            Map<String, Double> result = new HashMap<>();

            double total = getTotalValue();

            if (total == 0) return result;

            for (Asset asset : getAssets()) {

                result.merge(
                        asset.getType(),
                        asset.getCurrentValue() / total * 100,
                        Double::sum);
            }

            return result;
        }
    }

    // =================================================
    // GENERIC PORTFOLIO BALANCER
    // =================================================

    static class PortfolioBalancer<T extends Asset> {

        private final Portfolio portfolio;

        public PortfolioBalancer(Portfolio portfolio) {

            this.portfolio = portfolio;
        }

        public String balance(RiskProfile profile)
                throws MarketClosedException,
                       OverexposureLimitException {

            if (!MarketSimulator.marketOpen) {

                throw new MarketClosedException(
                        "Market is closed. Cannot rebalance.");
            }

            if (profile == null) {

                throw new IllegalArgumentException(
                        "Select a risk profile.");
            }

            // Generic collection of assets
            List<T> assets = portfolio.getAssets().stream()
                    .filter(a -> a.getCurrentValue() > 0)
                    .map(a -> (T) a)
                    .collect(Collectors.toList());

            if (assets.isEmpty()) {

                return "No assets available for balancing.";
            }

            double[] weights;

            switch (profile) {

                case CONSERVATIVE:

                    weights = new double[]{
                            0.30, 0.60, 0.10
                    };

                    break;

                case MODERATE:

                    weights = new double[]{
                            0.60, 0.30, 0.10
                    };

                    break;

                default:

                    weights = new double[]{
                            0.80, 0.10, 0.10
                    };
            }

            double total = portfolio.getTotalValue();

            double[] targets = {
                    total * weights[0],
                    total * weights[1],
                    total * weights[2]
            };

            Map<String, Double> current = new HashMap<>();

            for (T asset : assets) {

                current.merge(
                        asset.getType(),
                        asset.getCurrentValue(),
                        Double::sum);
            }

            StringBuilder output = new StringBuilder();

            output.append("REBALANCING RESULT\n");

            output.append(
                    "========================\n");

            String[] types = {
                    "Stock",
                    "Bond",
                    "Commodity"
            };

            for (int i = 0; i < types.length; i++) {

                double present =
                        current.getOrDefault(types[i], 0.0);

                double target = targets[i];

                output.append("\n")
                        .append(types[i])
                        .append("\nCurrent: ₹")
                        .append(String.format("%.2f", present))
                        .append("\nTarget: ₹")
                        .append(String.format("%.2f", target))
                        .append("\nDifference: ₹")
                        .append(String.format(
                                "%.2f", target - present))
                        .append("\n");
            }

            output.append(
                    "\nAllocation plan generated successfully.");

            output.append(
                    "\nSelected profile: ").append(profile);

            output.append(
                    "\n\nNote: This is a simulated allocation plan.");

            return output.toString();
        }
    }

    // =================================================
    // CUSTOM EXCEPTIONS
    // =================================================

    static class MarketClosedException extends Exception {

        public MarketClosedException(String message) {

            super(message);
        }
    }

    static class OverexposureLimitException extends Exception {

        public OverexposureLimitException(String message) {

            super(message);
        }
    }

    // =================================================
    // MULTITHREADING - MARKET SIMULATOR
    // =================================================

    static class MarketSimulator {

        private final Portfolio portfolio;

        private ScheduledExecutorService scheduler;

        static volatile boolean marketOpen = true;

        private final Random random = new Random();

        // Callback used to refresh GUI
        private Runnable onUpdate;

        public MarketSimulator(Portfolio portfolio) {

            this.portfolio = portfolio;
        }

        // Register GUI update callback
        public void setOnUpdate(Runnable onUpdate) {

            this.onUpdate = onUpdate;
        }

        // Start automatic simulation
        public synchronized void start() {

            if (scheduler != null &&
                    !scheduler.isShutdown()) {

                return;
            }

            scheduler =
                    Executors.newScheduledThreadPool(2);

            scheduler.scheduleAtFixedRate(
                    this::simulateOnce,
                    2,
                    3,
                    TimeUnit.SECONDS);
        }

        // Stop automatic simulation
        public synchronized void stop() {

            if (scheduler != null) {

                scheduler.shutdownNow();

                scheduler = null;
            }
        }

        // Generate random market price changes
        public void simulateOnce() {

            if (!marketOpen) return;

            List<Asset> assets = portfolio.getAssets();

            for (Asset asset : assets) {

                synchronized (asset) {

                    double oldPrice =
                            asset.getCurrentPrice();

                    // Random change between -2% and +2%
                    double change =
                            (random.nextDouble() * 0.04) - 0.02;

                    double newPrice =
                            oldPrice * (1 + change);

                    if (newPrice > 0) {

                        asset.setCurrentPrice(newPrice);
                    }
                }
            }

            // IMPORTANT FIX:
            // Refresh GUI safely on Swing Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {

                if (onUpdate != null) {

                    onUpdate.run();
                }
            });
        }
    }
}