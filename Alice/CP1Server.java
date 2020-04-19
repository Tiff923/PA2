import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CP1Server {

	public static void main(String[] args) throws IOException {

		int port = 4321;
		if (args.length > 0)
			port = Integer.parseInt(args[0]);

		// waits for requests to come in over the network
		ServerSocket welcomeSocket = null;
		// implements client sockets
		Socket connectionSocket = null;
		// write data to output stream
		DataOutputStream toClient = null;
		// read data from underlying inputstream
		DataInputStream fromClient = null;

		// output stream for writing data to FILE/fd
		FileOutputStream fileOutputStream = null;
		// writing data to underlying output stream w/o making a call to it
		BufferedOutputStream bufferedFileOutputStream = null;

		try {
			System.out.println("Establishing connection...");
			welcomeSocket = new ServerSocket(port);

			connectionSocket = welcomeSocket.accept();
			fromClient = new DataInputStream(connectionSocket.getInputStream());
			toClient = new DataOutputStream(connectionSocket.getOutputStream());

			// reads text from a character-input stream
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			//BufferedWriter outputWriter = new BufferedWriter(new OutputStreamWriter(connectionSocket.getOutputStream()));

			// prints formatted representations of objects to a text-output stream
			// PrintWriter output = new
			// PrintWriter(connectionSocket.getOutputStream(),true);

			while (!connectionSocket.isClosed()) {

				String request = inputReader.readLine();
				if (request.equals("Requesting authentication...")) {
					System.out.println("Client: " + request);
					break;
				} else {
					System.out.println("Request failed...");
				}
			}

			/* Create VerifyServer object */
			ServerVerification verifyServer = new ServerVerification(
					"/Users/alicekham/Desktop/50.005/PA2/Alice/server_signedpublickey.crt");

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

			/* Wait For Verification To Be Done */
			System.out.println("Waiting for verification completion...");
			System.out.println("Client: " + inputReader.readLine());

			/* Start File Transfer */
			System.out.println("Authentication Protocol complete...");
			System.out.println("Receiving File...");

			/* Get file size from Client */
			int fileSize = fromClient.readInt();
			System.out.println("Size of file from Client: " + fileSize);
			
			int size = 0;
			int count = 0;
			while (size < fileSize) {
				int packetType = fromClient.readInt();

				/* Packet For Transferring File Name */
				if (packetType == 0) {
					System.out.println("Receiving file...");
					int numBytes = fromClient.readInt();
					byte[] filename = new byte[numBytes];
					// Must use read fully!
					// See: https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
					fromClient.readFully(filename, 0, numBytes);

					fileOutputStream = new FileOutputStream("recv_" + new String(filename, 0, numBytes));
					bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

				/* Packet For Transferrin A Chunk Of File */
				} else if (packetType == 1) {
					count++;
					int numBytes = fromClient.readInt();
					int decrytpedNumBytes = fromClient.readInt();
					size += decrytpedNumBytes;
					
					byte[] block = new byte[numBytes];
					fromClient.readFully(block);

					/*Decrypt each 128 block */
					byte[] decryptedBlock = verifyServer.decryptFile(block);

					if (numBytes > 0) {
						//write(byte[] b, int off, int len) 
						//Writes len bytes from the specified byte array starting at offset off to this buffered output stream.
						bufferedFileOutputStream.write(decryptedBlock, 0, decrytpedNumBytes);
						bufferedFileOutputStream.flush();
					}
				}
			}

			/* End Of Transfer To Client */
			System.out.println("File transfer is done...");
			/* Close The Connection */
			System.out.println("Closing connection...");
			/* Close Streams and Sockets */
			bufferedFileOutputStream.close();
			fileOutputStream.close();
			fromClient.close();
			toClient.close();
			connectionSocket.close();

		} catch (Exception e) { e.printStackTrace();}
	}
}
