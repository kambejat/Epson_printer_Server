---

**Receipt Printing Server**

This Java code defines a server application that handles receipt printing requests from client applications. The server listens for incoming POST requests containing receipt data, processes the data, and prints the receipt either to a serial port or a selected printer.

**Overview**

The server application provides the following functionalities:

- Receives receipt data via HTTP POST requests.
- Processes the received JSON data to extract relevant information.
- Formats the receipt using the EscPos library.
- Prints the receipt to either a serial port or a selected printer.
- Includes a user-friendly Swing UI for configuration.

**Usage**

*Prerequisites:*

- Java Development Kit (JDK) installed
- Serial printer or a compatible printer connected to the system

*Setup and Run:*

1. Clone the repository:
   ```
   git clone https://github.com/kambejat/Epson_printer_Server.git
   ```

2. Compile the Java code:
   ```
   javac PrintServer.java
   ```

3. Run the server:
   ```
   java PrintServer
   ```

4. Access the server UI at `http://localhost:8080` to configure the printer and serial port.

*API Endpoint:*

Send a POST request to `/api/print` with the following JSON data:

```json
{
  "id": 1,
  "user_name": "John Doe",
  "total": 50.00,
  "waiterName": "Alice",
  "lineItems": [
    {
      "product": "Item 1",
      "price": 20.00,
      "quantity": 2
    },
    {
      "product": "Item 2",
      "price": 10.00,
      "quantity": 1
    }
  ]
}
```
