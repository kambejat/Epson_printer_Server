package com.pos_client;

import org.json.JSONArray;
import org.json.JSONObject;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import java.io.IOException;
import java.text.DecimalFormat;

public class ReceiptFormat {

    public static String formatReceipt(JSONObject jsonData) {
        int id = jsonData.getInt("id");
        String userName = jsonData.getString("user_name");
        double total = jsonData.getDouble("total");
        String waiterName = jsonData.optString("waiterName", "Not Set");

        JSONArray lineItemsArray = jsonData.getJSONArray("lineItems");

        // Create a receipt with escpos-coffee
        try {
            EscPos escpos = new EscPos(new PrinterOutputStream());

            Style title = new Style()
                    .setFontSize(Style.FontSize._3, Style.FontSize._3)
                    .setJustification(EscPosConst.Justification.Center);
            Style subtitle = new Style(escpos.getStyle())
                    .setBold(true)
                    .setUnderline(Style.Underline.OneDotThick)
                    .setJustification(EscPosConst.Justification.Center);
            Style bold = new Style(escpos.getStyle())
                    .setBold(true);
            Style centerText = new Style(escpos.getStyle())
                    .setBold(true)
                    .setJustification(EscPosConst.Justification.Center);

            escpos.writeLF(title, "Blues")
                    .feed(2)
                    .writeLF(centerText, "Blantyre Malawi")
                    .feed(1)
                    .writeLF(centerText, "Tel: +265881245047")
                    .feed(1)
                    .writeLF(centerText, "Email: chriskapanga@gmail.com")
                    .feed(1)
                    .writeLF("Date:   " + new java.util.Date())
                    .feed(2)
                    .writeLF("Pro - form Invoice")
                    .feed(2)
                    .writeLF(centerText, "Sale:          " + id)
                    .feed(1)
                    .writeLF(centerText, "Operator:    " + userName)
                    .feed(1)
                    .writeLF(centerText, "Served By:    " + waiterName)
                    .feed(1)
                    .writeLF(subtitle, "Opened On:  " + new java.util.Date())
                    .feed(1)
                    .writeLF(bold, "Item            Qty       Each      Total");

            for (int i = 0; i < lineItemsArray.length(); i++) {
                JSONObject lineItem = lineItemsArray.getJSONObject(i);
                String product = lineItem.getString("product");
                double price = lineItem.getDouble("price");
                int quantity = lineItem.getInt("quantity");

                // Format the columns with consistent spacing
                String formattedProduct = String.format("%-15s", product);
                String formattedQuantity = String.format("%-10s", quantity);

                // Format price with MK and remove leading zeros
                DecimalFormat decimalFormat = new DecimalFormat("MK#.##");
                String formattedPrice = String.format("%-10s", decimalFormat.format(price));

                // Calculate total and format with MK and remove leading zeros
                double totalAmount = price * quantity;
                String formattedTotal = String.format("%-10s", decimalFormat.format(totalAmount));

                // Combine formatted strings
                String lineItemString = formattedProduct + formattedQuantity + formattedPrice + formattedTotal;

                // Print the line item
                escpos.writeLF(lineItemString);
                escpos.feed(1);
            }

            escpos.feed(1);
            // Calculate and print total
            double vatTax = total * 0.165; // 165% VAT tax for example
            escpos.writeLF("Total Charges:             Mk" + String.format("%.2f", total));
            escpos.writeLF("Total (Inc):               Mk" + String.format("%.2f", total));
            escpos.feed(2);
            escpos.writeLF("This amount Includes");
            escpos.writeLF("VAT Tax (16.5%):   MK" + String.format("%.2f", vatTax));

            escpos.feed(2);

            // Print signature and thank you
            escpos.writeLF("Signature: ___________________________");
            escpos.feed(4);
            escpos.writeLF(centerText, "Thank you for visiting us!");
            escpos.feed(4);
            escpos.writeLF("");

            // Cut the paper (if supported by the printer)
            escpos.cut(EscPos.CutMode.PART);

            // Close the EscPos instance and release resources
            escpos.close();

            // Build text representation of the receipt for saving to file
            StringBuilder receipt = new StringBuilder();

            for (int i = 0; i < lineItemsArray.length(); i++) {
                JSONObject lineItem = lineItemsArray.getJSONObject(i);
                String product = lineItem.getString("product");
                double price = lineItem.getDouble("price");
                int quantity = lineItem.getInt("quantity");

                // Format the columns with consistent spacing
                String formattedProduct = String.format("%-15s", product);
                String formattedQuantity = String.format("%-10s", quantity);
                String formattedPrice = String.format("%-10.2f", price);

                // Create the formatted line
                String formattedLine = String.format("49,1,%s, 1 ,%s,%s, , ;", formattedProduct, formattedPrice,
                        formattedQuantity);

                // Append the formatted line to the receipt
                receipt.append(formattedLine);

                // Append newline if not the last line item
                if (i < lineItemsArray.length() - 1) {
                    receipt.append("\n");
                }
            }

            receipt.append("\n53,1,______,_,__;\n");
            receipt.append("56,1,______,_,__;\n");

            // Define the file path where you want to save the receipt
            String filePath = "receipt.txt";

            // Save the receipt to a text file
            PrinterUtils.saveToFile(receipt.toString(), filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Receipt printed to the printer successfully";
    }
}
