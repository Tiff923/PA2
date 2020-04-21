package Alice;

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
            
            certificate = serverCert.getEncoded(); 
            //returns encoded form of the certificate 
            // return type: byte[] form
            
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
    
    /* Get Private Key from File */
    public PrivateKey get() throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/PA2/Alice/private_key.der"));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public byte[] getCertificate() {
        return certificate;
    }

    /* Encrypt using Private Key */

    public byte[] encryptFile(byte[] fileByte) {

        byte[] encrypted = null;
        try {
            Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            eCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            encrypted = eCipher.doFinal(fileByte);
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
        return encrypted;
    }
    /* Decrypt using Private Key */
    public byte[] decryptFile(byte[] fileByte) {
        
        byte[] decrypted = null;
        try{
            Cipher dCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            dCipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            decrypted = dCipher.doFinal(fileByte);
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