import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.*;

public class PrivateKeyReader {

    public static PrivateKey get(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/private_key.der"));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}