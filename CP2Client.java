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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class CP2Client {

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
			out.println("Authentication complete ...");
			out.flush();

            System.out.println("Sending file...");
			
			//Setting up confidentiality protocol 
			ClientVerification encrypt = new ClientVerification("cacse.crt"); 

			//Generate session key 
			encrypt.generateSymmetricKey();

			//Encrypt session key with server public key 
			byte[] encryptedSessionKey = encrypt.encryptKey(); 

			//Send encrypted Session key to Server 
			toServer.writeInt(0);
			toServer.writeInt(encryptedSessionKey.length);
			toServer.write(encryptedSessionKey);
			toServer.flush();
		   
			System.out.println("I am here");
			// Send the filename
			toServer.writeInt(1);
			System.out.println("No, i am here already");
			toServer.writeInt(filename.getBytes().length);
			toServer.write(filename.getBytes());
			toServer.flush();

			//Encrypt file with session key
			byte[] fileToSend = Files.readAllBytes(Paths.get(filename));
			byte[] encryptedFile = encrypt.encryptFile2(fileToSend); 
			InputStream inputStream = new ByteArrayInputStream(encryptedFile); 

			// Open the file
			//fileInputStream = new FileInputStream(filename);
			bufferedFileInputStream = new BufferedInputStream(inputStream);

	        byte [] fromFileBuffer = new byte[117];

	        // Send the file
	        for (boolean fileEnded = false; !fileEnded;) {
				numBytes = bufferedFileInputStream.read(fromFileBuffer);
				fileEnded = numBytes < 117;

				toServer.writeInt(1);
				toServer.writeInt(numBytes);
				toServer.write(fromFileBuffer);
				toServer.flush();
			}

	        bufferedFileInputStream.close();
	        //fileInputStream.close();

			System.out.println("Closing connection...");

		} catch (Exception e) {e.printStackTrace();}

		long timeTaken = System.nanoTime() - timeStarted;
		System.out.println("Program took: " + timeTaken/1000000.0 + "ms to run");
	}
}
