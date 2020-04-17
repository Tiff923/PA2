import java.io.*;
import java.nio.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.FileInputStream;

public class PublicKeyReader {

    public static PublicKey get(String filename) throws Exception {

        byte[] keyBytes = Files
                .readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/public_key.der"));

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        
        /*
        --------------------------------------------
        obtain server's public key from certificate using X509Certificate class
        --------------------------------------------
        */
        //to extract public key, create CertificateFactory object
        InputStream fis = new FileInputStream("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/PA2/server_signedpublickey.crt");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate CAcert = (X509Certificate) cf.generateCertificate(fis);
        //extract public key from this object
        PublicKey key = CAcert.getPublicKey();
        CAcert.checkValidity();
        CAcert.verify(key);

        return kf.generatePublic(spec);
    }
}
