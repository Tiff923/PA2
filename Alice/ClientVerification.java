import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class ClientVerification {

    private InputStream CA;
    private static PublicKey CAPublicKey;
    private static PublicKey serverPublicKey;
    private static X509Certificate serverCert;
    private static SecretKey symmetricKey;
    private static byte[] nonce = new byte[32];
    private static byte[] encryptedNonce = new byte[128];

    public ClientVerification(String CA) throws CertificateException, IOException {

        this.CA = new FileInputStream(CA);
        /* Extract CA's Public Key from CA Cert */
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate CAcert = (X509Certificate) cf.generateCertificate(this.CA);
        CAPublicKey = CAcert.getPublicKey();
        /* Close CA File Input Stream */
        this.CA.close();
    }

    /* Get The Certificate From Server */
    public void getCertificate(InputStream certificate) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        serverCert = (X509Certificate) cf.generateCertificate(certificate);
    }

    public void getServerPublicKey() {
        serverPublicKey = serverCert.getPublicKey();
    }

    public void generateNonce() {
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
    }

    /** Decrypting encrypted nonce with publicKey **/
    public byte[] decryptNonce(byte[] encryptedNonce) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher dCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        dCipher.init(Cipher.DECRYPT_MODE, serverPublicKey);
        return dCipher.doFinal(encryptedNonce);
    }

    /** Check that decrypted nonce is equal to original **/
    public boolean validateNonce(byte[] decryptedNonce) {
        return Arrays.equals(nonce, decryptedNonce);
    }

    public byte[] getEncryptedNonce() {
        return encryptedNonce;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void verifyCert() {
        try {
            serverCert.checkValidity();
            serverCert.verify(CAPublicKey);
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    public void generateSymmetricKey() throws NoSuchAlgorithmException {
        symmetricKey = KeyGenerator.getInstance("AES").generateKey();
    }

    // Encrypt symmetric key using server public key
    public byte[] encryptKey() {
        byte[] encrypted = null;
        try {
            byte[] keyBytes = symmetricKey.getEncoded();
            System.out.println("Symmetric Key Length: " + keyBytes.length);

            Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            encrypted = eCipher.doFinal(keyBytes);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    /* Encrypt CP1 Using Server Public Key */
    public byte[] encryptFile1(byte[] fileByte) throws IOException {
        byte[] encrypted = null;
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            int start = 0;
            int fileLen = fileByte.length;
            Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            while (start < fileLen) {
                byte[] buffer;
                if (fileLen - start >= 117) {
                    buffer = eCipher.doFinal(fileByte, start, 117);
                } else {
                    buffer = eCipher.doFinal(fileByte, start, fileLen - start);
                }
                byteOutput.write(buffer, 0, buffer.length);
                start += 117;
            }

            encrypted = byteOutput.toByteArray();
            byteOutput.close();

            // byte[] encrypted = null;

            // try{
            // Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            // eCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            // encrypted= eCipher.doFinal(fileByte);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return encrypted;
    }

    /* Encrypt CP1 Using Server Public Key */
    public byte[] encryptFile(byte[] fileByte) throws IOException {
        byte[] encrypted = null;
        byte[] input = new byte[117];
        byte[] intermediate = null;
        ByteArrayOutputStream storage = new ByteArrayOutputStream();
        int i = 0;

        try {
            Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            while (i != (fileByte.length - 1)) {

                // Data max length == 117 bytes
                // System.out.println("i" + i);

                if ((fileByte.length - i - 1) >= 117) {
                    for (int j = 0; j < input.length; j++) {
                        input[j] = fileByte[i];
                        i++;
                    }
                } else {
                    int left = fileByte.length - 1 - i;
                    for (int j = 0; j < left; j++) {
                        input[j] = fileByte[i];
                        i++;
                    }
                }

                // Encryption of 117 byte data(one block)
                intermediate = eCipher.doFinal(input);

                // Store each block of data in order
                storage.write(intermediate);
            }

            // Retrieve all encrypted blocks
            encrypted = storage.toByteArray();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        System.out.println(encrypted);
        return encrypted;
    }

    // Encrypt CP2 using symmetric key
    public byte[] encryptFile2(byte[] fileByte) {
        byte[] encrypted = null;

        try {
            Cipher eCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            encrypted = eCipher.doFinal(fileByte);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return encrypted;
    }

}

// public static PublicKey get(String filename) throws Exception {

// byte[] keyBytes = Files
// .readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/public_key.der"));

// X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
// KeyFactory kf = KeyFactory.getInstance("RSA");

// /*
// * --------------------------------------------
// * obtain server's public key from
// * certificate using X509Certificate class
// * --------------------------------------------
// */
// // to extract public key, create CertificateFactory object
// InputStream fis = new FileInputStream(
// "/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/PA2/server_signedpublickey.crt");
// CertificateFactory cf = CertificateFactory.getInstance("X.509");
// X509Certificate CAcert = (X509Certificate) cf.generateCertificate(fis);
// // extract public key from this object
// PublicKey key = CAcert.getPublicKey();
// CAcert.checkValidity();
// CAcert.verify(key);

// return kf.generatePublic(spec);
// }
