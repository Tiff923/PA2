import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ServerVerification {

    private static PublicKey serverPublicKey = null;
    private static PrivateKey serverPrivateKey = null;
    private static byte[] certificate = null;
    private InputStream server;

    public ServerVerification(String server) throws IOException {
        this.server = new FileInputStream(server);
        try {
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            /* Get signed certificate */
            X509Certificate serverCert = (X509Certificate) cf.generateCertificate(this.server);
            certificate = serverCert.getEncoded(); //return byte[]
            /* Get Server Public Key */
            serverPublicKey = serverCert.getPublicKey();
            /* Get Server Private Key */
            serverPrivateKey = get("/Users/alicekham/Desktop/50.005/PA2/Alice/private_key.der");

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.server.close();
    }
    
    /* Get Private Key from file */
    public static PrivateKey get(String filename) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/private_key.der"));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public byte[] getCertificate() {
        return certificate;
    }
    /* CP1 Decryption using Private Key */
    public byte[] decryptFile(byte[] fileByte) {
        
        byte[] decrypted = null;
        try{
            Cipher fdCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            fdCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            decrypted = fdCipher.doFinal(fileByte);
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return decrypted;
    }
}