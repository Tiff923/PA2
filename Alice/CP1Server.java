import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class CP1Server {

	public static void main(String[] args) throws IOException {

		int port = 4321;
		if (args.length > 0)
			port = Integer.parseInt(args[0]);

		/*
		 * Server Socket - is created to bind() to a port and listen() for a connect()
		 * from a client. So a server just waits for a conversation and doesn't start
		 * one.
		 * 
		 * Client Socket - is created to connect() to a listen() server. The client
		 * initiates the connection.
		 */

		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		// write data to output stream
		DataOutputStream toClient = null;
		// read data from underlying inputstream
		DataInputStream fromClient = null;

		// output stream for writing data to FILE/fd
		FileOutputStream fileOutputStream = null;
		// writing data to underlying output stream w/o making a call to it
		BufferedOutputStream bufferedFileOutputStream = null;

		try {
			// System.out.println("Establishing connection...");
			serverSocket = new ServerSocket(port);
			clientSocket = serverSocket.accept();
			fromClient = new DataInputStream(clientSocket.getInputStream());
			toClient = new DataOutputStream(clientSocket.getOutputStream());

			// reads text from a character-input stream
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(fromClient));
			PrintWriter outputWriter = new PrintWriter(toClient, true);
			// BufferedWriter outputWriter = new BufferedWriter(new
			// OutputStreamWriter(clientSocket.getOutputStream()));

			while (!clientSocket.isClosed()) {

				String request = inputReader.readLine();
				if (request.equals("Requesting authentication...")) {
					System.out.println("Client: " + request);
					break;
				} else {
					System.out.println("Request failed...");
				}
			}

			/* Setting Up Protocol */
			ServerVerification verifyServer = new ServerVerification(
					"/Users/alicekham/Desktop/50.005/PA2/Alice/server_signedkey.crt");

			/* Get Nonce From Client */
			fromClient.read(verifyServer.getNonce());
			// outputWriter.println("Nonce received...");
			System.out.println("Nonce received...");

			/* Encrypt Nonce */
			verifyServer.encryptNonce();
			System.out.println("Nonce encrypted...");

			/* Send Encrypted Nonce To Client */
			// outputWriter.println("Sending nonce...");
			toClient.write(verifyServer.getEncryptedNonce());
			System.out.println("Nonce sent to client...");
			toClient.flush();

			/* Receive Cert request from Client */
			while (true) {
				String request = inputReader.readLine();
				if (request.equals("Requesting certificate from server...")) {
					System.out.println("Client: " + request);

					/* Send Cert to Client */
					toClient.write(verifyServer.getCertificate());
					System.out.println("Certificate sent to client...");
					toClient.flush();
					break;
				} else {
					System.out.println("Request failed...");
				}
			}

			/* Receive confirmation from Client */
			String ok = inputReader.readLine();
			if (ok.equals("Server identity verified...")) {
				System.out.println("Server identity verified...");
			} else {
				System.out.println("Client failed to identify server...");
			}

			/* Send Client An Encrypted Message */
			/* Hi I'm The Server */
			// while (true) {
			// String request = inputReader.readLine();
			// if (request.equals("Please provide server identity...")) {
			// System.out.println("Client: " + request);

			// /* Send Encrypted Message */
			// String msg = "I'm the server.";
			// byte[] msgByte = verifyServer.encryptFile(msg.getBytes());
			// System.out.println(msgByte);
			// toClient.write(msgByte, 0, msgByte.length);
			// toClient.flush();
			// break;
			// } else {
			// System.out.println("Failed to send message...");
			// }
			// }

			/* Wait For Verification To Be Done */
			// System.out.println("Waiting for verification completion...");
			// System.out.println("Client: " + inputReader.readLine());

			/* Start File Transfer */
			/* Start File Transfer */
			System.out.println("Authentication Protocol complete...");
			System.out.println("Receiving File...");

			/* Get file size from Client */
			int fileSize = fromClient.readInt();
			System.out.println("Size of file from Client: " + fileSize);

			String filename = "";
			while (!clientSocket.isClosed()) {

				int packetType = fromClient.readInt();

				/* Packet For Transferring File Name */
				if (packetType == 0) {
					/*
					 * System.out.println("Receiving file..."); int numBytes = fromClient.readInt();
					 * byte[] filename = new byte[numBytes]; // Must use read fully! // See: //
					 * https://stackoverflow.com/questions/25897627/datainputstream-read-vs-
					 * datainputstream-readfully fromClient.readFully(filename, 0, numBytes);
					 * fileOutputStream = new FileOutputStream("recv_" + new String(filename, 0,
					 * numBytes)); bufferedFileOutputStream = new
					 * BufferedOutputStream(fileOutputStream);
					 */

					int filenameLen = fromClient.readInt();
					byte[] filenameBytes = new byte[filenameLen];
					fromClient.readFully(filenameBytes);
					filename = new String(filenameBytes);

					/* Packet For Transferrin A Chunk Of File */
				} else if (packetType == 1) {

					int encryptedNumBytes = fromClient.readInt();
					System.out.println("File size: " + encryptedNumBytes);
					FileOutputStream file = new FileOutputStream("recv_" + filename, true);

					if (encryptedNumBytes == 128) {
						byte[] encrypted = new byte[encryptedNumBytes];
						fromClient.readFully(encrypted, 0, encryptedNumBytes);

						System.out.println(Arrays.toString(encrypted));
						System.out.println("Length of eFileBytes: " + encrypted.length);

						byte[] decrypted = verifyServer.decryptFileCP1(encrypted);
						System.out.println(Arrays.toString(decrypted));
						file.write(decrypted);
						file.close();

					} else if (encryptedNumBytes < 128) {
						byte[] encrypted = new byte[encryptedNumBytes];
						fromClient.readFully(encrypted, 0, encryptedNumBytes);

						System.out.println(Arrays.toString(encrypted));
						System.out.println("Length of eFileBytes: " + encrypted.length);

						byte[] decrypted = verifyServer.decryptFileCP1(encrypted);
						System.out.println(Arrays.toString(decrypted));
						file.write(decrypted);
						file.close();

						/* End Of File Transfer */
						System.out.println("Transfer complete...");
						outputWriter.println("Ending Transfer...");
						System.out.println("Closing all connections...");
						fromClient.close();
						toClient.close();
						clientSocket.close();
					}
				} else if (packetType == 2) {
					fromClient.close();
					toClient.close();
					clientSocket.close();
				}
				/* Start File Transfer */
				// System.out.println("Authentication Protocol complete...");
				// System.out.println("Receiving File...");

				// /* Get file size from Client */
				// int fileSize = fromClient.readInt();
				// System.out.println("Size of file from Client: " + fileSize);

				// int size = 0;
				// String filename = "";

				// while (size < fileSize) {

				// int task = fromClient.readInt();

				// /* Packet For Transferring File Name */
				// if (task == 1) {
				// /*
				// * System.out.println("Receiving file..."); int numBytes =
				// fromClient.readInt();
				// * byte[] filename = new byte[numBytes]; // Must use read fully! // See: //
				// * https://stackoverflow.com/questions/25897627/datainputstream-read-vs-
				// * datainputstream-readfully fromClient.readFully(filename, 0, numBytes);
				// *
				// * fileOutputStream = new FileOutputStream("recv_" + new String(filename, 0,
				// * numBytes)); bufferedFileOutputStream = new
				// * BufferedOutputStream(fileOutputStream);
				// */

				// byte[] filenameBytes = new byte[fileSize];
				// fromClient.readFully(filenameBytes);
				// filename = new String(filenameBytes);

				// /* INCOMING BLOCKS */
				// } else if (task == 2) {

				// // System.out.println("Receiving file...");
				// // System.out.println("Getting file size...");

				// int eFileSize = fromClient.readInt();
				// System.out.println("File size: " + eFileSize);
				// FileOutputStream file = new FileOutputStream("recv_" + filename, true);

				// if (eFileSize == 117) {
				// byte[] eFileBytes = new byte[eFileSize];
				// fromClient.readFully(eFileBytes, 0, eFileSize);

				// System.out.println(Arrays.toString(eFileBytes));
				// System.out.println("Length of eFileBytes: " + eFileBytes.length);

				// /* Decrypt with PUBLIC KEY */
				// // System.out.println("Decrypting file with session key...");
				// byte[] decrypted = verifyServer.decryptFileCP1(eFileBytes);
				// file.write(decrypted);
				// file.close();

				// } else if (eFileSize < 117) {
				// byte[] eFileBytes = new byte[eFileSize];
				// fromClient.readFully(eFileBytes, 0, eFileSize);

				// System.out.println(Arrays.toString(eFileBytes));
				// System.out.println("Length of eFileBytes: " + eFileBytes.length);

				// byte[] decrypted = verifyServer.decryptFileCP1(eFileBytes);
				// file.write(decrypted);
				// file.close();

				// /* End Of File Transfer */
				// System.out.println("Transfer complete...");
				// outputWriter.println("Ending Transfer...");
				// System.out.println("Closing all connections...");
				// fromClient.close();
				// toClient.close();
				// clientSocket.close();
			}

		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}
}
