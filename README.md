<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body {
            font-family: Arial, sans-serif;
            max-width: 800px;
            margin: 0 auto;
            padding: 20px;
        }

        h1 {
            color: #007bff;
        }

        h2 {
            color: #007bff;
        }

        p {
            line-height: 1.6;
        }

        code {
            background-color: #f8f9fa;
            border: 1px solid #ccc;
            padding: 2px 6px;
            border-radius: 4px;
        }

        .container {
            margin-top: 20px;
            padding: 20px;
            background-color: #f8f9fa;
            border: 1px solid #ccc;
        }
    </style>
    <title>Receipt Printing Server</title>
</head>

<body>

    <h1>Receipt Printing Server</h1>

    <p>This Java code defines a server application that handles receipt printing requests from client applications.
        The server listens for incoming POST requests containing receipt data, processes the data, and prints the receipt
        either to a serial port or a selected printer.</p>

    <h2>Overview</h2>

    <p>The server application provides the following functionalities:</p>

    <ul>
        <li>Receives receipt data via HTTP POST requests.</li>
        <li>Processes the received JSON data to extract relevant information.</li>
        <li>Formats the receipt using the EscPos library.</li>
        <li>Prints the receipt to either a serial port or a selected printer.</li>
        <li>Includes a user-friendly Swing UI for configuration.</li>
    </ul>

    <h2>Usage</h2>

    <div class="container">

        <h3>Prerequisites</h3>

        <ul>
            <li>Java Development Kit (JDK) installed</li>
            <li>Serial printer or a compatible printer connected to the system</li>
        </ul>

        <h3>Setup and Run</h3>

        <p>Follow these steps to set up and run the server:</p>

        <ol>
            <li>Clone the repository:</li>
            <code>git clone https://github.com/kambejat/Epson_printer_Server.git</code>

            <li>Compile the Java code:</li>
            <code>javac PrintServer.java</code>

            <li>Run the server:</li>
            <code>java PrintServer</code>

            <li>Access the server UI at <code>http://localhost:8080</code> to configure the printer and serial port.</li>
        </ol>

        <h3>API Endpoint</h3>

        <p>Send a POST request to <code>/api/print</code> with the following JSON data:</p>

        <pre>
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
        </pre>

    </div>

    <h2>License</h2>

    <p>This project is licensed under the MIT License - see the <a
            href="https://github.com/kambejat/Epson_printer_Server.git">LICENSE</a> file for details.</p>

</body>

</html>
