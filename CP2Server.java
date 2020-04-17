import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class CP2Server {

    public static void main(String[] args) {

        int port = 4001;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);

    // sockets: waits for requests to come in over the network
    ServerSocket welcomeSocket = null;
    Socket connectionSocket = null;
    DataOutputStream toClient = null;
    DataInputStream fromClient = null;

    String filepath = "/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/PA2/server_signedpublickey.crt";

    FileOutputStream fileOutputStream = null;
    BufferedOutputStream bufferedFileOutputStream = null;

    try
    {
        welcomeSocket = new ServerSocket(port);

        connectionSocket = welcomeSocket.accept();
        // Returns an input stream for this socket.
        fromClient = new DataInputStream(connectionSocket.getInputStream());
        // Returns an output stream for this socket.
        toClient = new DataOutputStream(connectionSocket.getOutputStream());

        // Reads text from a character-input stream, buffering characters so 
        // as to provide for the efficient reading of characters, arrays, and lines.
        BufferedReader inputReader = new BufferedReader(
            new InputStreamReader(connectionSocket.getInputStream()));

        while (!connectionSocket.isClosed()) {

            // int packetType = fromClient.readInt();

            //incoming request from client
            String request = inputReader.readLine();
            if (request.equals("Requesting authentication...")) {
                System.out.println("Client: " + request);
                break;
            } else {
                System.out.println("Request failed...");
            }

            //server verification
            Server
            //getting nonce from client
            

            // If the packet is for transferring the filename
            // if (packetType == 0) {

                System.out.println("Receiving file...");

                int numBytes = fromClient.readInt();
                byte[] filename = new byte[numBytes];
                // Must use read fully!
                // See:
                // https://stackoverflow.com/questions/25897627/datainputstream-read-vs-datainputstream-readfully
                fromClient.readFully(filename, 0, numBytes);

                fileOutputStream = new FileOutputStream("recv_" + new String(filename, 0, numBytes));
                bufferedFileOutputStream = new BufferedOutputStream(fileOutputStream);

                // If the packet is for transferring a chunk of the file
            } else if (packetType == 1) {

                int numBytes = fromClient.readInt();
                byte[] block = new byte[numBytes];
                fromClient.readFully(block, 0, numBytes);

                if (numBytes > 0)
                    bufferedFileOutputStream.write(block, 0, numBytes);

                if (numBytes < 117) {
                    System.out.println("Closing connection...");

                    if (bufferedFileOutputStream != null)
                        bufferedFileOutputStream.close();
                    if (bufferedFileOutputStream != null)
                        fileOutputStream.close();
                    fromClient.close();
                    toClient.close();
                    connectionSocket.close();
                }
            }
    }catch(
    Exception e)
    {
        e.printStackTrace();
    }
    
}
