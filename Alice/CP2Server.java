import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CP2Server {

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        DataOutputStream toClient = null;
        DataInputStream fromClient = null;

        String signedCert = ("/Users/alicekham/Desktop/50.005/PA2/Alice/server_signedkey.crt");

        BufferedReader inputReader = null;
        PrintWriter outputWriter = null;

        try {
            serverSocket = new ServerSocket(4321);
            clientSocket = serverSocket.accept();
            fromClient = new DataInputStream(clientSocket.getInputStream());
            toClient = new DataOutputStream(clientSocket.getOutputStream());

            inputReader = new BufferedReader(new InputStreamReader(fromClient));
            outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            while (true) {
                String request = inputReader.readLine();
                if (request.equals("Requesting authentication...")) {
                    System.out.println("Client: " + request);
                    break;
                } else {
                    System.out.println("Request failed...");
                }
            }

            /* Set Up */
            ServerVerification serverVerification = new ServerVerification(signedCert);

            /* Get Nonce From Client */
			fromClient.read(serverVerification.getNonce());
			// outputWriter.println("Nonce received...");
			System.out.println("Nonce received...");

			/* Encrypt Nonce */
			serverVerification.encryptNonce();
			System.out.println("Nonce encrypted...");

			/* Send Encrypted Nonce To Client */
			// outputWriter.println("Sending nonce...");
			toClient.write(serverVerification.getEncryptedNonce());
			System.out.println("Nonce sent to client...");
            toClient.flush();
            
            /* Receive Cert request from client */
            while (true) {
                String request = inputReader.readLine();
                if (request.equals("Requesting certificate from server...")) {
                    System.out.println("Client: " + request);

                    /* Send Cert to Client */
                    toClient.write(serverVerification.getCertificate());
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

            /* Wait for verification to be done */
            // System.out.println("Waiting for verification completion...");
            // System.out.println("Client: " + inputReader.readLine());

            System.out.println("Authentication Protocol complete...");
			System.out.println("Receiving File...");

            byte[] sessionKey;
            String filename = "";
            Cipher dCipher = Cipher.getInstance("AES/ECB/NoPadding");

            /* Wait for client to accept the encrypted session key */
            while (!clientSocket.isClosed()) {
                System.out.println("there");
                int task = fromClient.readInt();
                System.out.println(task);
                System.out.println("reached");
                BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());

                /* Decrypt Session Key using Server Public Key */
                if (task == 0) {
                    int sessionKeySize = fromClient.readInt();
                    sessionKey = new byte[sessionKeySize];
                    fromClient.readFully(sessionKey);

                    System.out.println("Receive sessionKey of size: " + sessionKeySize);

                    String base64 = Base64.getEncoder().encodeToString(sessionKey);
                    System.out.println("Decrypting sessionKey...");

                    byte[] sessionKeyBytes = serverVerification.decryptFileCP2(sessionKey);
                    SecretKey secretKey = new SecretKeySpec(sessionKeyBytes, 0, sessionKeyBytes.length, "AES");
                    dCipher.init(Cipher.DECRYPT_MODE, secretKey);
                }

                /* Get File Name */
                else if (task == 1) {
                    int filenameLen = fromClient.readInt();
                    byte[] filenameBytes = new byte[filenameLen];
                    fromClient.readFully(filenameBytes);
                    filename = new String(filenameBytes);
                }

                /* Receive File */
                else if (task == 2) {
                    System.out.println("Receiving file...");
                    System.out.println("Getting file size...");

                    int eFileSize = fromClient.readInt();
                    System.out.println("File size: " + eFileSize);
                    FileOutputStream file = new FileOutputStream("recv_" + filename, true);

                    if (eFileSize == 128) {
                        byte[] eFileBytes = new byte[eFileSize];
                        fromClient.readFully(eFileBytes, 0, eFileSize);

                        System.out.println(Arrays.toString(eFileBytes));
                        System.out.println("Length of eFileBytes: " + eFileBytes.length);

                        /* Decrypt with session key */
                        System.out.println("Decrypting file with session key...");
                        byte[] decrypted = dCipher.doFinal(eFileBytes);
                        file.write(decrypted);
                        file.close();

                    } else if (eFileSize < 128) {
                        byte[] eFileBytes = new byte[eFileSize];
                        fromClient.readFully(eFileBytes, 0, eFileSize);

                        System.out.println(Arrays.toString(eFileBytes));
                        System.out.println("Length of eFileBytes: " + eFileBytes.length);

                        /* Decrypt with session key */
                        System.out.println("Decrypting file with session key...");
                        byte[] decrypted = dCipher.doFinal(eFileBytes);
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
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }
}