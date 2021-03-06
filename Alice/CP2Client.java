import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class CP2Client {

	public static void main(String[] args) {

		// String filename = "500.txt";
		Scanner in = new Scanner(System.in);
		String filename = in.nextLine();
		in.close();
		if (args.length > 0)
			filename = args[0];

		String serverAddress = "localhost";
		if (args.length > 1)
			filename = args[1];

		int port = 4321;
		if (args.length > 2)
			port = Integer.parseInt(args[2]);

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

			// To send data through the socket to the server
			PrintWriter outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);
			

			outputWriter.println("Requesting authentication...");
			// outputWriter.flush();

			ClientVerification obtain = new ClientVerification("cacse.crt");
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
			outputWriter.println("Requesting certificate from server...");

			// get cert from Server and store in buffer
			byte[] buffer = new byte[8192];
			fromServer.read(buffer);

			// Obtain server public key
			InputStream certificate = new ByteArrayInputStream(buffer);
			obtain.getCertificate(certificate);
			obtain.getServerPublicKey();
			obtain.verifyCert();

			byte[] check = obtain.decryptNonce(obtain.getEncryptedNonce());

			if (obtain.validateNonce(check) == true) {
				outputWriter.println("Server identity verified...");
				System.out.println("Server identity verified...");
			} else {
				System.out.println("Server identity not verified...");
				// DO WHAT ELSE?
				System.out.println("Closing connections...");
				clientSocket.close();
				fromServer.close();
				toServer.close();
			}

			// Authentication Protocol - decrypt message from server
			// Inform server authentication complete
			outputWriter.println("Authentication Protocol complete...");
			// outputWriter.flush();

			System.out.println("Sending file...");

			// Setting up confidentiality protocol
			ClientVerification encrypt = new ClientVerification("cacse.crt");

			// Generate session key
			encrypt.generateSymmetricKey();

			// Encrypt session key with server public key
			byte[] encryptedSessionKey = encrypt.encryptKey();

			// Send encrypted Session key to Server
			toServer.writeInt(0);
			toServer.writeInt(encryptedSessionKey.length);
			toServer.write(encryptedSessionKey);
			toServer.flush();

			// Send the filename
			toServer.writeInt(1);
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			// Encrypt file with session key
			byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			byte[] encryptedFile = encrypt.encryptFile2(fileToSend);
			InputStream inputStream = new ByteArrayInputStream(encryptedFile);

			// Open the file
			// fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(inputStream);

			byte[] fromFileBuffer = new byte[128];

			// Send the file
			for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < 128;

				toServer.writeInt(2);
				toServer.writeInt(numBytes);
				toServer.write(fromFileBuffer);
				toServer.flush();
			}

			bufferedFileInputStream.close();
			// fileInputStream.close();

			System.out.println("File transfer done...");
			System.out.println("Closing connection...");
			clientSocket.close();
			fromServer.close();
			toServer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken / 1000000.0 + "ms to run");
	}
}
