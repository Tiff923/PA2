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
import java.util.Arrays;

public class CP1Client {

	public static void main(String[] args) {

    	String filename = "500.txt";
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

			ClientVerification obtain = new ClientVerification("/Users/alicekham/Desktop/50.005/PA2/Alice/cacse.crt"); 
			System.out.println("Requesting authentication...");
			out.println("Requesting authentication...");

			/** Generate Nonce **/
            System.out.println("Generating Nonce...");
            obtain.generateNonce();

            /** Send Nonce to Server **/
            System.out.println("Sending Nonce over...");
            toServer.write(obtain.getNonce());

            /** Get Encrypted nonce **/
            fromServer.read(obtain.getEncryptedNonce());
            System.out.println("Retrieved encrypted nonce from server...");

			System.out.println("Requesting certificate from server...");
			out.println("Requesting certificate from server...");

			//get cert from Server and store in buffer 
			byte[] buffer = new byte[8192]; 
			fromServer.read(buffer); 
			
			//Obtain server public key 
			InputStream certificate = new ByteArrayInputStream(buffer); 
			obtain.getCertificate(certificate);
			obtain.getServerPublicKey();
			obtain.verifyCert();
			
			byte[] check = obtain.decryptNonce(obtain.getEncryptedNonce());

			if (obtain.validateNonce(check) == true) {
				out.println("Server identity verified...");
				System.out.println("Server identity verified...");
			} else {
				System.out.println("Server identity not verified...");
				// DO WHAT ELSE?
				System.out.println("Closing connections...");
				clientSocket.close();
				fromServer.close();
				toServer.close();
			}

			System.out.println("Sending file...");

			byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			toServer.writeInt(fileToSend.length);

			// Send the filename
			toServer.writeInt(0);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Open the file
			fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(fileInputStream);

	        byte [] fromFileBuffer = new byte[117];

	        // Send the file
	        for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				byte[] encrypted = obtain.encryptFile1(fromFileBuffer); 
				System.out.println(Arrays.toString(encrypted));
				int encryptedNumBytes = encrypted.length; 
				fileEnded = numBytes < 117;

				System.out.println("File size: " + encryptedNumBytes); 
				

				toServer.writeInt(1);
				toServer.writeInt(encryptedNumBytes);
				toServer.write(encrypted);
				toServer.flush();
			}

			toServer.writeInt(2);
			toServer.flush();

	        bufferedFileInputStream.close();
	        fileInputStream.close();

			System.out.println("Closing connection...");

			// System.out.println("Sending file...");

			// // Send File Size
			// toServer.writeInt(0);
			// toServer.writeInt(filename.getBytes().length);
			// toServer.flush();

			// // Send File Name
			// toServer.writeInt(1);
			// System.out.println("Im sending name");
			// toServer.write(filename.getBytes());
			// toServer.flush();

			// // Encrypt File with Public Key
			// byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			// byte[] encryptedFile = obtain.encryptFile1(fileToSend);
			// InputStream inputStream = new ByteArrayInputStream(encryptedFile);
			
			// // Open the file
			// // fileInputStream = new FileInputStream(filename);
			// bufferedFileInputStream = new BufferedInputStream(inputStream);

	        // byte [] fromFileBuffer = new byte[117];

	        // // Send the file
	        // for (boolean fileEnded = false; !fileEnded;) {
			// 	numBytes = bufferedFileInputStream.read(fromFileBuffer); 
			// 	// int encryptedNumBytes = encrypted.length; 
			// 	fileEnded = numBytes < 117;

			// 	toServer.writeInt(2);
			// 	toServer.writeInt(numBytes);
			// 	// toServer.writeInt(encryptedNumBytes);
			// 	toServer.write(fromFileBuffer);
			// 	toServer.flush();
			// }

	        // bufferedFileInputStream.close();
	        // inputStream.close();

			// System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
