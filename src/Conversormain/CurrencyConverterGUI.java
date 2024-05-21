package Conversormain;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurrencyConverterGUI extends JFrame {
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/2645c80d572acafb3ef6e8ba/latest/USD";
    private static final Map<String, String> CURRENCIES = new HashMap<>();
    private static final String HISTORY_FILE = "conversion_history.json";
    private final List<ConversionRecord> history = new ArrayList<>();

    static {
        CURRENCIES.put("ARS", "Peso argentino");
        CURRENCIES.put("BOB", "Boliviano boliviano");
        CURRENCIES.put("BRL", "Real brasileño");
        CURRENCIES.put("CLP", "Peso chileno");
        CURRENCIES.put("COP", "Peso colombiano");
        CURRENCIES.put("USD", "Dólar estadounidense");
        CURRENCIES.put("EUR", "Euro");
        CURRENCIES.put("GBP", "Libra esterlina");
        CURRENCIES.put("JPY", "Yen japonés");
        CURRENCIES.put("AUD", "Dólar australiano");
        CURRENCIES.put("CAD", "Dólar canadiense");
        CURRENCIES.put("CHF", "Franco suizo");
        CURRENCIES.put("CNY", "Yuan chino");
        CURRENCIES.put("INR", "Rupia india");
        CURRENCIES.put("MXN", "Peso mexicano");
        CURRENCIES.put("RUB", "Rublo ruso");
        CURRENCIES.put("KRW", "Won surcoreano");
        CURRENCIES.put("NZD", "Dólar neozelandés");
        CURRENCIES.put("SGD", "Dólar de Singapur");
        CURRENCIES.put("TRY", "Lira turca");
        CURRENCIES.put("ZAR", "Rand sudafricano");
    }

    private JComboBox<String> fromCurrencyComboBox;
    private JComboBox<String> toCurrencyComboBox;
    private JTextField amountTextField;
    private JLabel resultLabel;
    private JTextArea historyTextArea;
    private JsonObject conversionRates;

    public CurrencyConverterGUI() {
        setTitle("Convertidor de Monedas");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;


        loadConversionRates();


        String[] currencyArray = CURRENCIES.keySet().stream()
                .map(code -> code + " - " + CURRENCIES.get(code))
                .toArray(String[]::new);

        // componentes de la interface
        fromCurrencyComboBox = new JComboBox<>(currencyArray);
        toCurrencyComboBox = new JComboBox<>(currencyArray);
        amountTextField = new JTextField();
        resultLabel = new JLabel("Resultados:");
        historyTextArea = new JTextArea(10, 30);
        historyTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(historyTextArea);

        JButton convertButton = new JButton("Convertir");
        convertButton.addActionListener(e -> convertCurrency());

        // agredandole componentes a la interface
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Desde:"), gbc);
        gbc.gridx = 1;
        add(fromCurrencyComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("A:"), gbc);
        gbc.gridx = 1;
        add(toCurrencyComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        add(amountTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(convertButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(resultLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(scrollPane, gbc);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadConversionRates() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
            conversionRates = jsonObject.getAsJsonObject("conversion_rates");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void convertCurrency() {
        try {
            String fromCurrency = ((String) fromCurrencyComboBox.getSelectedItem()).split(" - ")[0];
            String toCurrency = ((String) toCurrencyComboBox.getSelectedItem()).split(" - ")[0];
            double amount = Double.parseDouble(amountTextField.getText());

            if (conversionRates.has(fromCurrency) && conversionRates.has(toCurrency)) {
                double fromRate = conversionRates.get(fromCurrency).getAsDouble();
                double toRate = conversionRates.get(toCurrency).getAsDouble();
                double convertedAmount = amount * (toRate / fromRate);

                String conversionResult = String.format("%.2f %s = %.2f %s", amount, fromCurrency, convertedAmount, toCurrency);
                resultLabel.setText(conversionResult);

                // Log the conversion with timestamp
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                String timestamp = dtf.format(now);

                historyTextArea.append(timestamp + " - " + conversionResult + "\n");

                // Guardar historial
                history.add(new ConversionRecord(timestamp, fromCurrency, toCurrency, amount, convertedAmount));
                saveHistoryToFile();
            } else {
                resultLabel.setText("Error: Moneda no encontrada.");
            }
        } catch (NumberFormatException e) {
            resultLabel.setText("Error: Cantidad inválida.");
        }
    }

    private void saveHistoryToFile() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CurrencyConverterGUI::new);
    }
}

class ConversionRecord {
    private String timestamp;
    private String fromCurrency;
    private String toCurrency;
    private double amount;
    private double convertedAmount;

    public ConversionRecord(String timestamp, String fromCurrency, String toCurrency, double amount, double convertedAmount) {
        this.timestamp = timestamp;
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.amount = amount;
        this.convertedAmount = convertedAmount;
    }


}

