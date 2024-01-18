package com.pos_client;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.net.httpserver.*;

import javax.swing.*;

import org.json.JSONObject;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class PrintServer {

	private static SerialPort serialPort;
	private static PrintService selectedPrinter;
	private static String serialPortName;
	private static Preferences prefs;
	private static HttpServer server;
	private static TrayIcon trayIcon;

	public static void main(String[] args) throws IOException {
		prefs = Preferences.userNodeForPackage(PrintServer.class);

		if (SystemTray.isSupported()) {
			createAndShowSystemTray();
		}

		// Create the server in a separate thread
		Thread serverThread = new Thread(() -> {
			try {
				server = HttpServer.create(new InetSocketAddress(8080), 0);
				server.createContext("/api/print", new PrintHandler());
				server.createContext("/api/pcname", new GetPCNameHandler());
				server.start();
				System.out.println("Server started on port 8080");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		serverThread.setDaemon(true);
		serverThread.start();

		SwingUtilities.invokeLater(() -> createAndShowUI());

		String savedPrinterName = prefs.get("selectedPrinter", "");
		String savedSerialPortName = prefs.get("serialPortName", "");

		if (!savedPrinterName.isEmpty()) {
			initializeSelectedPrinter(savedPrinterName);
			FiscalPrint.initializeFiscalPrinter();
		}
		if (!savedSerialPortName.isEmpty()) {
			initializeSerialPort(savedSerialPortName);
			// FiscalPrint.initializeFiscalPrinter(savedSerialPortName);
		}
	}

	private static void restoreWindow() {
		SwingUtilities.invokeLater(() -> createAndShowUI());
	}

	private static void createAndShowSystemTray() {
		try {
			// Create a popup menu components
			PopupMenu popup = new PopupMenu();
			MenuItem openItem = new MenuItem("Open");
			MenuItem exitItem = new MenuItem("Exit");

			// Add components to the popup menu
			popup.add(openItem);
			popup.add(exitItem);

			// Create a tray icon
			trayIcon = new TrayIcon(new ImageIcon(
					"C:\\Users\\Prag\\Work\\BluesInventory\\frontend\\Extention\\printer_pos\\src\\main\\java\\com\\pos_client\\printer.png")
					.getImage(), "Print Server", popup);
			trayIcon.setImageAutoSize(true);

			// Add the tray icon to the system tray
			SystemTray.getSystemTray().add(trayIcon);

			// Add action listeners
			openItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreWindow();
				}
			});

			trayIcon.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					restoreWindow();
				}
			});

			exitItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stopServer();
					System.exit(0);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopServer() {
		if (server != null) {
			server.stop(0);
			System.out.println("Server stopped.");
		}
	}

	public static void restartServer() {
		stopServer();
		startServer();
	}

	public static void startServer() {
		Thread serverThread = new Thread(() -> {
			try {
				server = HttpServer.create(new InetSocketAddress(8080), 0);
				server.createContext("/api/print", new PrintHandler());
				server.createContext("/api/pcname", new GetPCNameHandler());
				server.createContext("/api/fiscal/print", new FiscalPrintHandler());
				server.start();
				System.out.println("Server started on port 8080");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		serverThread.setDaemon(true);
		serverThread.start();
	}

	public static void createAndShowUI() {
		JFrame frame = new JFrame("Receipt Printer Extender Server");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(460, 300);
		frame.setLayout(new FlowLayout());

		JTextArea receiptTextArea = new JTextArea(5, 30);
		receiptTextArea.setLineWrap(true);

		JComboBox<String> serialPortDropdown = new JComboBox<>();
		PrinterUtils.populateSerialPortList(serialPortDropdown);

		JComboBox<String> printerDropdown = new JComboBox<>();
		PrinterUtils.populatePrinterList(printerDropdown);

		JButton setSerialPortButton = new JButton("Set Serial Port");
		setSerialPortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedSerialPortName = (String) serialPortDropdown.getSelectedItem();
				initializeSerialPort(selectedSerialPortName);
				serialPortName = selectedSerialPortName;
				prefs.put("serialPortName", serialPortName);
			}
		});

		JButton saveSerialPortButton = new JButton("Save Serial Port");
		saveSerialPortButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedSerialPortName = (String) serialPortDropdown.getSelectedItem();
				saveSerialPortConfiguration(selectedSerialPortName);
				frame.setVisible(false);
				minimizeToSystemTray();
			}
		});

		JButton printButton = new JButton("Test Print");
		printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String receiptContent = PrinterUtils.generateSampleReceipt();
				System.out.println("Printing receipt:\n" + receiptContent);
				PrinterUtils.saveToFile(receiptContent, "receipt.txt");

				sendDataToSelectedPrinter(receiptContent);
			}
		});

		JButton savePrinterButton = new JButton("Save Printer");
		savePrinterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedPrinterName = (String) printerDropdown.getSelectedItem();
				savePrinterConfiguration(selectedPrinterName);
				initializeSelectedPrinter(selectedPrinterName);
				frame.setVisible(false);
				minimizeToSystemTray();
			}
		});

		frame.add(new JLabel("Serial Port:"));
		frame.add(serialPortDropdown);
		frame.add(setSerialPortButton);
		frame.add(saveSerialPortButton);
		frame.add(new JLabel("Printer:"));
		frame.add(printerDropdown);
		frame.add(printButton);
		frame.add(savePrinterButton);
		frame.setVisible(true);
	}

	private static void minimizeToSystemTray() {
		try {
			trayIcon.displayMessage("Print Server", "Print Server is now running in the background.",
					TrayIcon.MessageType.INFO);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	static class FiscalPrintHandler implements HttpHandler {
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
					FiscalPrint.sendReceiptToFiscalPrinter(receiptContent);

					// Allow requests from any origin (replace "*" with your actual frontend URL in
					// production)
					Headers headers = exchange.getResponseHeaders();
					headers.add("Access-Control-Allow-Origin", "*");
					headers.add("Access-Control-Allow-Methods", "POST"); // Add other allowed HTTP methods if
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
					FiscalPrint.sendReceiptToFiscalPrinter(receiptContent);

					// Allow requests from any origin (replace "*" with your actual frontend URL in
					// production)
					Headers headers = exchange.getResponseHeaders();
					headers.add("Access-Control-Allow-Origin", "*");
					headers.add("Access-Control-Allow-Methods", "POST"); // Add other allowed HTTP methods if
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

	}

	// Custom HTTP handler for printing requests
	static class PrintHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			Headers headers = exchange.getResponseHeaders();
			headers.add("Access-Control-Allow-Origin", "*");
			headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
			headers.add("Access-Control-Allow-Headers", "Content-Type");
			headers.add("Access-Control-Max-Age", "3600");

			if ("POST".equals(exchange.getRequestMethod())) {
				try {
					Scanner scanner = new Scanner(exchange.getRequestBody());
					StringBuilder requestBody = new StringBuilder();
					while (scanner.hasNextLine()) {
						requestBody.append(scanner.nextLine());
					}

					String receiptContent = requestBody.toString();

					System.out.println("Received receipt content: " + receiptContent);

					if (serialPort != null) {
						sendReceiptToSerialPort(receiptContent);
						String response = "Receipt printed to the serial port successfully";
						exchange.sendResponseHeaders(200, response.length());
						OutputStream os = exchange.getResponseBody();
						os.write(response.getBytes());
						os.close();
					} else if (selectedPrinter != null) {
						sendReceiptToPrinter(receiptContent);
						String response = "Receipt printed to the printer successfully";
						exchange.sendResponseHeaders(200, response.length());
						OutputStream os = exchange.getResponseBody();
						os.write(response.getBytes());
						os.close();
					} else {
						String response = "No serial port or printer selected. Please set one and try again.";
						exchange.sendResponseHeaders(400, response.length());
						OutputStream os = exchange.getResponseBody();
						os.write(response.getBytes());
						os.close();
					}
				} catch (Exception e) {
					// Log detailed error information
					e.printStackTrace();
					String response = "Error processing the receipt: " + e.getMessage();
					exchange.sendResponseHeaders(500, response.length());
					OutputStream os = exchange.getResponseBody();
					os.write(response.getBytes());
					os.close();
				}
			} else {
				exchange.sendResponseHeaders(405, -1); // Method Not Allowed for non-POST requests
			}
		}

		// Send the receipt content to the selected serial port
		private void sendReceiptToSerialPort(String receiptContent) {
			JSONObject jsonData = new JSONObject(receiptContent);
			ReceiptFormat.formatReceipt(jsonData);
		}

		// Send the receipt content to the selected printer
		private void sendReceiptToPrinter(String receiptContent) {
			JSONObject jsonData = new JSONObject(receiptContent);
			ReceiptFormat.formatReceipt(jsonData);
		}
	}

	static class GetPCNameHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			// Allow requests from any origin (replace "*" with your actual frontend URL in
			// production)
			Headers headers = exchange.getResponseHeaders();
			headers.add("Access-Control-Allow-Origin", "*");
			headers.add("Access-Control-Allow-Methods", "GET"); // Only allow GET requests for this API
			headers.add("Access-Control-Allow-Headers", "Content-Type");
			headers.add("Access-Control-Max-Age", "3600");

			// Get the computer name
			String pcName = InetAddress.getLocalHost().getHostName();

			// Respond to the client with the PC name
			String response = "PC Name: " + pcName;
			exchange.sendResponseHeaders(200, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.getBytes());
			os.close();
		}
	}

}
