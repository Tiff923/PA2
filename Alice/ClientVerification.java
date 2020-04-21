import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
    public byte[] encryptKey(){
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
    public byte[] encryptFile(byte[] fileByte) throws IOException {
        byte[] encrypted = null;
        byte[] input = new byte[117];
        byte[] intermediate = null; 
        ByteArrayOutputStream storage = new ByteArrayOutputStream();
        int i = 0; 
          
        try {
            Cipher eCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            eCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            while(i != (fileByte.length -1)){

                //Data max length == 117 bytes 
                    // System.out.println("i" + i); 
                    
                    if( (fileByte.length-i-1)>= 117 ){
                        for (int j=0; j<input.length; j++){
                            input[j] = fileByte[i]; 
                            i++; 
                        }
                    }
                    else{
                        int left = fileByte.length-1-i; 
                        for(int j=0; j<left; j++){
                            input[j] = fileByte[i]; 
                            i++; 
                        }
                    }
                
                //Encryption of 117 byte data(one block)
                intermediate = eCipher.doFinal(input);

                //Store each block of data in order 
                storage.write(intermediate);
            }

            //Retrieve all encrypted blocks 
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

    //Encrypt CP2 using symmetric key
    public byte[] encryptFile2 (byte[] fileByte){   
        byte[] encrypted = null;

        try{
            Cipher eCipher = Cipher.getInstance("AES/ECB/PKCS5Padding"); 
            eCipher.init(Cipher.ENCRYPT_MODE, symmetricKey); 
            encrypted= eCipher.doFinal(fileByte); 

        }catch (NoSuchAlgorithmException e) {
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

    //     byte[] keyBytes = Files
    //             .readAllBytes(Paths.get("/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/public_key.der"));

    //     X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
    //     KeyFactory kf = KeyFactory.getInstance("RSA");

    //     /*
    //      * -------------------------------------------- 
    //      * obtain server's public key from
    //      * certificate using X509Certificate class
    //      * --------------------------------------------
    //      */
    //     // to extract public key, create CertificateFactory object
    //     InputStream fis = new FileInputStream(
    //             "/Users/alicekham/Desktop/50.005/ProgrammingAssignment2/PA2/server_signedpublickey.crt");
    //     CertificateFactory cf = CertificateFactory.getInstance("X.509");
    //     X509Certificate CAcert = (X509Certificate) cf.generateCertificate(fis);
    //     // extract public key from this object
    //     PublicKey key = CAcert.getPublicKey();
    //     CAcert.checkValidity();
    //     CAcert.verify(key);

    //     return kf.generatePublic(spec);
    // }
