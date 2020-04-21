import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CP1Client {

	public static void main(String[] args) {

    	String filename = "100.txt";
    	if (args.length > 0) filename = args[0];

    	String serverAddress = "localhost";
    	if (args.length > 1) filename = args[1];

    	int port = 4321;
    	if (args.length > 2) port = Integer.parseInt(args[2]);

		int numBytes = 0;

		Socket clientSocket = null;

        DataOutputStream toServer = null;
        DataInputStream fromServer = null;

    	FileInputStream fileInputStream = null;
        BufferedInputStream bufferedFileInputStream = null;

		long timeStarted = System.nanoTime();

		try {

			System.out.println("Establishing connection to server...");

			// Connect to server and get the input and output streams
			clientSocket = new Socket(serverAddress, port);
			toServer = new DataOutputStream(clientSocket.getOutputStream());
			fromServer = new DataInputStream(clientSocket.getInputStream());

            /*---------------------
            Setting Up Protocol
            ---------------------*/
			//To send data through the socket to the server
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); 
			//To get the server's response
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			out.println("Requesting authentication...");
			out.println("Requesting certificate from server...");
			out.flush();

			//get cert from Server and store in buffer 
			byte[] buffer = new byte[8192]; 
			fromServer.read(buffer); 
			
			//Obtain server public key 
			ClientVerification obtain = new ClientVerification("cacse.crt"); 
			InputStream certificate = new ByteArrayInputStream(buffer); 
			obtain.getCertificate(certificate);
			obtain.getServerPublicKey();
			obtain.verifyCert();

			//Authentication Protocol - decrypt message from server 
			//Inform server authentication complete 
			//out.println("Authentication complete ...");
			//out.flush();

			// //Encrypt file with server's private key 
			// byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			// System.out.println(fileToSend.length);
			// byte[] encryptedFile = obtain.encryptFile(fileToSend); 
			// System.out.println(encryptedFile.length); 
			// InputStream inputStream = new ByteArrayInputStream(encryptedFile); 
			// System.out.print(inputStream.available());

			System.out.println("Sending file...");

			byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			toServer.writeInt(fileToSend.length);

			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			//toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];

	        // Send the file
	        for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				byte[] encrypted = obtain.encryptFile1(fromFileBuffer); 
				int encryptedNumBytes = encrypted.length; 
				fileEnded = numBytes < 117;

				System.out.println("File size: " + encryptedNumBytes); 


				toServer.writeInt(1);
				toServer.writeInt(encryptedNumBytes);
				toServer.write(encrypted);
				toServer.flush();
			}

	        bufferedFileInputStream.close();
	        fileInputStream.close();

			System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
