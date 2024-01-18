package com.pos_client;

import com.fazecast.jSerialComm.SerialPort;
import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.output.PrinterOutputStream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JComboBox;

public class PrinterUtils {

	private static SerialPort serialPort;
	private static PrintService selectedPrinter;

	// Initialize and open the selected serial port
	public static void initializeSerialPort(String serialPortName) {
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
	public static void initializeSelectedPrinter(String printerName) {
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

	public static void sendDataToSelectedPrinter(String data) {
		if (selectedPrinter != null) {
			try {
				javax.print.DocPrintJob printJob = selectedPrinter.createPrintJob();
				javax.print.DocFlavor flavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE;
				javax.print.Doc doc = new javax.print.SimpleDoc(data.getBytes(), flavor, null);
				printJob.print(doc, null);
				System.out.println("Data sent to printer: " + selectedPrinter.getName());
			} catch (javax.print.PrintException e) {
				e.printStackTrace();
			}
		} else if (serialPort != null) {
			serialPort.writeBytes(data.getBytes(), data.getBytes().length);
			System.out.println("Data sent to serial port: " + serialPort.getSystemPortName());
		} else {
			System.err.println("Neither printer nor serial port is initialized.");
		}
	}

	public static void populateSerialPortList(JComboBox<String> serialPortDropdown) {
		SerialPort[] ports = SerialPort.getCommPorts();
		for (SerialPort port : ports) {
			serialPortDropdown.addItem(port.getSystemPortName());
		}
	}

	// Populate the printer dropdown with available printer names
	public static void populatePrinterList(JComboBox<String> printerDropdown) {
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService service : services) {
			printerDropdown.addItem(service.getName());
		}
	}

	public static void savePrinterConfiguration(String printerName, Preferences prefs) {
		prefs.put("selectedPrinter", printerName);
		System.out.println("Saved selected printer: " + printerName);
	}

	public static void saveSerialPortConfiguration(String serialPortName, Preferences prefs) {
		prefs.put("serialPortName", serialPortName);
		System.out.println("Saved selected serial port: " + serialPortName);
	}

	public static void saveToFile(String data, String filePath) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
			writer.write(data);
			System.out.println("Data saved to " + filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String generateSampleReceipt() {
		// Create a sample receipt content
		StringBuilder receipt = new StringBuilder();
		receipt.append("Sample Receipt\n");
		receipt.append("--------------------------\n");
		receipt.append("Item 1          Mk10.00\n");
		receipt.append("Item 1          Mk20.00\n");

		try {
			EscPos escpos = new EscPos(new PrinterOutputStream());
			escpos.writeLF("Sample Receipt\n");
			escpos.feed(1);
			escpos.writeLF("--------------------------\n");
			escpos.feed(1);
			escpos.writeLF("Item 1          Mk10.00\n");
			escpos.writeLF("Item 1          Mk20.00\n");
			escpos.writeLF("Item 1          Mk30.00\n");
			escpos.writeLF("--------------------------\n");
			escpos.writeLF("Total           Mk60.00\n");
			escpos.writeLF("");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return receipt.toString();
	}

}
