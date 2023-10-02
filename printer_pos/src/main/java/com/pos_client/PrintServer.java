package com.pos_client;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.net.httpserver.*;

import javax.swing.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.prefs.Preferences;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

public class PrintServer {

	private static SerialPort serialPort;
	private static PrintService selectedPrinter; // Store the selected printer
	private static String serialPortName; // Store the selected serial port name
	private static Preferences prefs;

	public static void main(String[] args) throws IOException {
        prefs = Preferences.userNodeForPackage(PrintServer.class);
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/print", new PrintHandler());
        server.start();
        System.out.println("Server started on port 8080");
        SwingUtilities.invokeLater(() -> createAndShowUI());

        String savedPrinterName = prefs.get("selectedPrinter", "");
        String savedSerialPortName = prefs.get("serialPortName", "");
        if (!savedPrinterName.isEmpty()) {
            initializeSelectedPrinter(savedPrinterName);
        }
        if (!savedSerialPortName.isEmpty()) {
            initializeSerialPort(savedSerialPortName);
        }
    }
	// Custom HTTP handler for printing requests
	static class PrintHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			// Only accept POST requests
			if ("POST".equals(exchange.getRequestMethod())) {
				// Get the request body (receipt content)
				Scanner scanner = new Scanner(exchange.getRequestBody());
				StringBuilder requestBody = new StringBuilder();
				while (scanner.hasNextLine()) {
					requestBody.append(scanner.nextLine());
				}

				String receiptContent = requestBody.toString();

				// Replace this with your actual JPOS code to print the receipt

				System.out.println("Received receipt content: " + receiptContent);

				if (serialPort != null) {
					// Send the receipt content to the saved serial port
					sendReceiptToSerialPort(receiptContent);

					// Allow requests from any origin (replace "*" with your actual frontend URL in
					// production)
					Headers headers = exchange.getResponseHeaders();
					headers.add("Access-Control-Allow-Origin", "*");
					headers.add("Access-Control-Allow-Methods", "POST, OPTIONS"); // Add other allowed HTTP methods if
																					// needed
					headers.add("Access-Control-Allow-Headers", "Content-Type");
					headers.add("Access-Control-Max-Age", "3600");

					// Respond to the client with a success message
					String response = "Receipt printed to the serial port successfully";
					exchange.sendResponseHeaders(200, response.length());
					OutputStream os = exchange.getResponseBody();
					os.write(response.getBytes());
					os.close();
				} else if (selectedPrinter != null) {
					// Send the receipt content to the selected printer
					sendReceiptToPrinter(receiptContent);

					// Allow requests from any origin (replace "*" with your actual frontend URL in
					// production)
					Headers headers = exchange.getResponseHeaders();
					headers.add("Access-Control-Allow-Origin", "*");
					headers.add("Access-Control-Allow-Methods", "POST, OPTIONS"); // Add other allowed HTTP methods if
																					// needed
					headers.add("Access-Control-Allow-Headers", "Content-Type");
					headers.add("Access-Control-Max-Age", "3600");

					// Respond to the client with a success message
					String response = "Receipt printed to the printer successfully";
					exchange.sendResponseHeaders(200, response.length());
					OutputStream os = exchange.getResponseBody();
					os.write(response.getBytes());
					os.close();
				} else {
					// If neither serial port nor printer is seslected, respond with an error
					// message
					String response = "No serial port or printer selected. Please set one and try again.";
					exchange.sendResponseHeaders(400, response.length());
					OutputStream os = exchange.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} else {
				// Return 405 Method Not Allowed for non-POST requests
				exchange.sendResponseHeaders(405, -1);
			}

		}

		// Send the receipt content to the selected serial port
		private void sendReceiptToSerialPort(String receiptContent) {
			JSONObject jsonData = new JSONObject(receiptContent);

			// Now you can access the individual fields in the JSON object
			int id = jsonData.getInt("id");
			String userName = jsonData.getString("user_name");
			// String paymentMethod = jsonData.getString("payment_method");
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
						.feed(3)
						.writeLF(centerText, "Blantyre Malawi")
						.feed(1)
						.writeLF(centerText, "Tel: +265881245047")
						.feed(1)
						.writeLF(centerText, "Email: chriskapanga@gmail.com")
						.feed(2)
						.writeLF("Date:   " + new java.util.Date())
						.feed(2)
						.writeLF("Pro - form Invoice")
						.feed(3)
						.writeLF(centerText, "Sale:          " + id)
						.feed(1)
						.writeLF(centerText, "Operator:    " + userName)
						.feed(1)
						.writeLF(centerText, "Served By:    " + waiterName)
						.feed(1)
						.writeLF(subtitle, "Opened On:  " + new java.util.Date())
						.feed(2)
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

				escpos.feed(2);
				// Calculate and print total
				double vatTax = total * 0.165; // 165% VAT tax for example
				escpos.writeLF("Total Charges:         Mk" + String.format("%.2f", total));
				escpos.writeLF("Total (Inc):           Mk" + String.format("%.2f", total));
				escpos.feed(2);
				escpos.writeLF("This amount Includes");
				escpos.writeLF("VAT Tax (16.5%):   MK" + String.format("%.2f", vatTax));

				escpos.feed(2);

				// Print signature and thank you
				escpos.writeLF("Signature: _________________________");
				escpos.feed(6);
				escpos.writeLF(centerText, "Thank you for visiting us!");
				escpos.feed(4);
				escpos.writeLF("");

				// Cut the paper (if supported by the printer)
				escpos.cut(EscPos.CutMode.PART);

				// Close the EscPos instance and release resources
				escpos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
			 	String formattedLine = String.format("49,1,%s, 1 ,%s,%s, , ;", formattedProduct, formattedPrice, formattedQuantity);
	
			 	// Append the formatted line to the receipt
			 	receipt.append(formattedLine);
	
			 	// Append newline if not the last line item
			 	if (i < lineItemsArray.length() - 1) {
			 		receipt.append("\n");
			 	}
			 }
                         
                         receipt.append("\n53,1,______,_,__;\n");
                         receipt.append("56,1,______,_,__;\n");
	
			// // Define the file path where you want to save the receipt
			 String filePath = "receipt.txt";
			 
	
			 // Save the receipt to a text file
			 saveToFile(receipt.toString(), filePath);
		}

		// Send the receipt content to the selected printer
		private void sendReceiptToPrinter(String receiptContent) {
			JSONObject jsonData = new JSONObject(receiptContent);

			// Now you can access the individual fields in the JSON object
			int id = jsonData.getInt("id");
			String userName = jsonData.getString("user_name");
			// String paymentMethod = jsonData.getString("payment_method");
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
						.feed(3)
						.writeLF(centerText, "Blantyre Malawi")
						.feed(1)
						.writeLF(centerText, "Tel: +265881245047")
						.feed(1)
						.writeLF(centerText, "Email: chriskapanga@gmail.com")
						.feed(2)
						.writeLF("Date:   " + new java.util.Date())
						.feed(2)
						.writeLF("Pro - form Invoice")
						.feed(3)
						.writeLF(centerText, "Sale:          " + id)
						.feed(1)
						.writeLF(centerText, "Operator:    " + userName)
						.feed(1)
						.writeLF(centerText, "Served By:    " + waiterName)
						.feed(1)
						.writeLF(subtitle, "Opened On:  " + new java.util.Date())
						.feed(2)
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

				escpos.feed(2);
				// Calculate and print total
				double vatTax = total * 0.165; // 165% VAT tax for example
				escpos.writeLF("Total Charges:         Mk" + String.format("%.2f", total));
				escpos.writeLF("Total (Inc):           Mk" + String.format("%.2f", total));
				escpos.feed(2);
				escpos.writeLF("This amount Includes");
				escpos.writeLF("VAT Tax (16.5%):   MK" + String.format("%.2f", vatTax));

				escpos.feed(2);

				// Print signature and thank you
				escpos.writeLF("Signature: _________________________");
				escpos.feed(6);
				escpos.writeLF(centerText, "Thank you for visiting us!");
				escpos.feed(4);
				escpos.writeLF("");

				// Cut the paper (if supported by the printer)
				escpos.cut(EscPos.CutMode.PART);

				// Close the EscPos instance and release resources
				escpos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// text build
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
			 	String formattedLine = String.format("49,1,%s, 1 ,%s,%s, , ;", formattedProduct, formattedPrice, formattedQuantity);
	
			 	// Append the formatted line to the receipt
			 	receipt.append(formattedLine);
	
			 	// Append newline if not the last line item
			 	if (i < lineItemsArray.length() - 1) {
			 		receipt.append("\n");
			 	}
			 }
                         
                         receipt.append("\n53,1,______,_,__;\n");
                         receipt.append("56,1,______,_,__;\n");
	
			// // Define the file path where you want to save the receipt
			 String filePath = "receipt.txt";
	
			 // Save the receipt to a text file
			 saveToFile(receipt.toString(), filePath);
		}
	}

	// Create and show the Swing UI
	public static void createAndShowUI() {
		JFrame frame = new JFrame("Receipt Printer Extender Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(460, 300);
		frame.setLayout(new FlowLayout());

		JTextArea receiptTextArea = new JTextArea(5, 30);
		receiptTextArea.setLineWrap(true);

		// Create a dropdown for selecting serial ports
		JComboBox<String> serialPortDropdown = new JComboBox<>();
		populateSerialPortList(serialPortDropdown); // Populate the dropdown with available serial ports

		// Add a dropdown for selecting printers
		JComboBox<String> printerDropdown = new JComboBox<>();
		populatePrinterList(printerDropdown); // Populate the dropdown with available printer names

		JButton setSerialPortButton = new JButton("Set Serial Port");
		setSerialPortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedSerialPortName = (String) serialPortDropdown.getSelectedItem();
				initializeSerialPort(selectedSerialPortName);
				serialPortName = selectedSerialPortName; // Update the selected serial port name
				prefs.put("serialPortName", serialPortName); // Save the serial port configuration
			}
		});

		JButton saveSerialPortButton = new JButton("Save Serial Port");
		saveSerialPortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedSerialPortName = (String) serialPortDropdown.getSelectedItem();
				saveSerialPortConfiguration(selectedSerialPortName); // Save the selected serial port
			}
		});

		JButton printButton = new JButton("Test Print Receipt");
		printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String receiptContent = generateSampleReceipt();
				System.out.println("Printing receipt:\n" + receiptContent);
				saveToFile(receiptContent, "receipt.txt");

				// Replace this with your actual JPOS code to print the receipt

				// Call a method to send data to the selected printer
				sendDataToSelectedPrinter(receiptContent);
			}
		});

		JButton savePrinterButton = new JButton("Save Printer");
		savePrinterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedPrinterName = (String) printerDropdown.getSelectedItem();
				savePrinterConfiguration(selectedPrinterName);
				initializeSelectedPrinter(selectedPrinterName); // Update the selected printer
			}
		});

		frame.add(new JLabel("Serial Port:"));
		frame.add(serialPortDropdown); // Add the serial port dropdown
		frame.add(setSerialPortButton);
		frame.add(saveSerialPortButton); // Add the save serial port button
		frame.add(new JLabel("Printer:"));
		frame.add(printerDropdown);
		frame.add(printButton);
		frame.add(savePrinterButton);
		frame.setVisible(true);
	}

	// Populate the serial port dropdown with available serial ports
	private static void populateSerialPortList(JComboBox<String> serialPortDropdown) {
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort port : ports) {
			serialPortDropdown.addItem(port.getSystemPortName());
		}
	}

	private static String generateSampleReceipt() {
		// Create a sample receipt content
		StringBuilder receipt = new StringBuilder();
		receipt.append("Sample Receipt\n");
		receipt.append("--------------------------\n");
		receipt.append("Item 1          Mk10.00\n");
		receipt.append("Item 1          Mk20.00\n");
                receipt.append("Item 1          Mk30.00\n");
                receipt.append("--------------------------\n");
                receipt.append("Total           Mk60.00\n");
		
		try {
			EscPos escpos = new EscPos(new PrinterOutputStream());
			escpos.writeLF(receipt.toString());
			escpos.feed(1);
			escpos.writeLF("");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return receipt.toString();
	}

	// Initialize and open the selected serial port
	private static void initializeSerialPort(String serialPortName) {
		if (serialPortName != null && !serialPortName.isEmpty()) {
			serialPort = SerialPort.getCommPort(serialPortName);
			serialPort.setBaudRate(9600);
			serialPort.setNumDataBits(8);
			serialPort.setNumStopBits(1);

			if (serialPort.openPort()) {
				System.out.println("Serial Port opened successfully.");

			} else {
				System.out.println("Error: Failed to open the serial port.");

			}
		}
	}

	// Initialize and open the selected printer
	private static void initializeSelectedPrinter(String printerName) {
		if (printerName != null && !printerName.isEmpty()) {
			PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
			for (PrintService service : services) {
				if (service.getName().equalsIgnoreCase(printerName)) {
					selectedPrinter = service;
					System.out.println("Selected printer: " + printerName);
					return;
				}
			}
		}

		System.err.println("Printer not found: " + printerName);
	}

	// Send data to the selected printer
	private static void sendDataToSelectedPrinter(String data) {
		if (selectedPrinter != null) {
			try {
				if (serialPort != null) {
					serialPort.writeBytes(data.getBytes(), data.getBytes().length);
					System.out.println("Data sent to serial port: " + serialPort.getSystemPortName());
				} else {
					System.err.println("Serial port is not initialized.");
				}
				javax.print.DocPrintJob printJob = selectedPrinter.createPrintJob();
				DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
				javax.print.Doc doc = new javax.print.SimpleDoc(data.getBytes(), flavor, null);
				printJob.print(doc, null);
				System.out.println("Data sent to printer: " + selectedPrinter.getName());

			} catch (javax.print.PrintException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Printer is not initialized.");
		}
	}

	// Populate the printer dropdown with available printer names
	private static void populatePrinterList(JComboBox<String> printerDropdown) {
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService service : services) {
			printerDropdown.addItem(service.getName());
		}
	}

	// Save the selected printer configuration to user preferences
	private static void savePrinterConfiguration(String printerName) {
		prefs.put("selectedPrinter", printerName);
		System.out.println("Saved selected printer: " + printerName);
		JOptionPane.showMessageDialog(null, "Printer configuration saved successfully!" + printerName, "Printer Saved",
				JOptionPane.INFORMATION_MESSAGE);
	}

	// Save the selected serial port configuration to user preferences
	private static void saveSerialPortConfiguration(String serialPortName) {
		prefs.put("serialPortName", serialPortName);
		// Show a dialog to inform the user that the serial port configuration is saved
		JOptionPane.showMessageDialog(null, "Serial port configuration saved successfully!" + serialPortName,
				"Serial Port Saved", JOptionPane.INFORMATION_MESSAGE);
	}

	private static void saveToFile(String data, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(data);
            System.out.println("Data saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
