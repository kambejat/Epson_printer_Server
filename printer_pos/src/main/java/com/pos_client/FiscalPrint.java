package com.pos_client;

import javax.print.PrintService;
import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;

import jpos.FiscalPrinter;
import jpos.JposException;

public class FiscalPrint {
    static FiscalPrinter fiscalPrinter;

    public static SerialPort serialPort;
    public static PrintService selectedPrinter;
    public static boolean fiscalPrinterInitialized = false;

    public FiscalPrint() {
        fiscalPrinter = new FiscalPrinter();
        fiscalPrinter.addStatusUpdateListener(null);
        fiscalPrinter.addDirectIOListener(null);
        initializeFiscalPrinter();
    }

    public static void sendReceiptToFiscalPrinter(String receiptContent) {
        JSONObject jsonData = new JSONObject(receiptContent);

        JSONArray lineItemsArray = jsonData.getJSONArray("lineItems");

        System.out.println(lineItemsArray);
        // Open Fiscal Printer
        initializeFiscalPrinter();

        ////////////////////////////////
        // RT Specific Commands - BEG //
        ////////////////////////////////

        boolean isRT = false;

        if (isRT) {
            // Fiscal Receipt
            try {
                fiscalPrinter.beginFiscalReceipt(true);
                for (int i = 0; i < lineItemsArray.length(); i++) {
                    JSONObject lineItem = lineItemsArray.getJSONObject(i);
                    String product = lineItem.getString("product");
                    double price = lineItem.getDouble("price");
                    int quantity = lineItem.getInt("quantity");

                    // Convert double values to int
                    int priceInt = (int) price;
                    // int totalPriceInt = (int) (price * quantity);

                    // Print each line item on the receipt
                    fiscalPrinter.printRecItem(product, priceInt, quantity, 1650, priceInt, "");
                }

                // Calculate total and print subtotal and total on the receipt
                int totalAmount = calculateTotal(lineItemsArray);
                fiscalPrinter.printRecSubtotal(totalAmount);
                fiscalPrinter.printRecTotal(totalAmount, totalAmount, "0Contante");

                fiscalPrinter.endFiscalReceipt(false);

            } catch (JposException je) {
                System.out.println("JposException - errorCode : " + je.getErrorCode() + " - errorCodeExtended : "
                        + je.getErrorCodeExtended());
            }

        }

    }

    private static int calculateTotal(JSONArray lineItemsArray) {
        int total = 0;
        for (int i = 0; i < lineItemsArray.length(); i++) {
            JSONObject lineItem = lineItemsArray.getJSONObject(i);
            double price = lineItem.getDouble("price");
            int quantity = lineItem.getInt("quantity");
            total += (int) (price * quantity); // Typecast the result to int
        }
        return total;
    }

    public static void initializeFiscalPrinter() {
        try {
            // Dynamically set the device name (you can set it based on user input or
            // configuration)
            String deviceName = "FP-700"; // Replace this with your dynamic device selection logic
            // Initialize the fiscal printer
            fiscalPrinter = new FiscalPrinter();
            fiscalPrinter.open(deviceName); // Replace "FiscalPrinter1" with your specific device name
            fiscalPrinter.claim(91000); // Claim the fiscal printer
            fiscalPrinter.setDeviceEnabled(true); // Enable the fiscal printer
            System.out.println("Fiscal Printer initialized successfully.");
            // You can add additional logic here if needed
        } catch (JposException e) {
            e.printStackTrace();
            // Handle JposException appropriately (log the error, display an error message,
            // etc.)
            JOptionPane.showMessageDialog(null, "Error: Failed to initialize Fiscal Printer.",
                    "Fiscal Printer Error", JOptionPane.ERROR_MESSAGE);
        }

    }

}
