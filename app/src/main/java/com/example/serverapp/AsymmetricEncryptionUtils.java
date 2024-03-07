package com.example.serverapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

public class AsymmetricEncryptionUtils {

    private static final String PREFS_NAME = "KeyPairPrefs";
    private static final String PUBLIC_KEY_KEY = "publicKey";
    private static final String PRIVATE_KEY_KEY = "privateKey";
    private static final String CERTIFICATE_KEY = "certificate";

    static {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private static X509Certificate certificate;

    public static void generateCertificateAndKeys(Context context) {
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            if (publicKey == null || privateKey == null || certificate == null) {
                // Try to retrieve from SharedPreferences
                byte[] publicKeyBytes = preferences.getString(PUBLIC_KEY_KEY, null) != null ?
                        hexStringToByteArray(preferences.getString(PUBLIC_KEY_KEY, null)) : null;

                byte[] privateKeyBytes = preferences.getString(PRIVATE_KEY_KEY, null) != null ?
                        hexStringToByteArray(preferences.getString(PRIVATE_KEY_KEY, null)) : null;

                byte[] certificateBytes = preferences.getString(CERTIFICATE_KEY, null) != null ?
                        hexStringToByteArray(preferences.getString(CERTIFICATE_KEY, null)) : null;

                if (publicKeyBytes != null && privateKeyBytes != null && certificateBytes != null) {
                    publicKey = (PublicKey) fromByteArray(publicKeyBytes);
                    privateKey = (PrivateKey) fromByteArray(privateKeyBytes);
                    certificate = (X509Certificate) fromByteArray(certificateBytes);
                    Log.d("exit", "Digital  exit");

                } else {
                    // Generate new keys and certificate
                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                    keyPairGenerator.initialize(512);
                    KeyPair keyPair = keyPairGenerator.generateKeyPair();

                    publicKey = keyPair.getPublic();
                    privateKey = keyPair.getPrivate();

                    certificate = generateCertificate(publicKey, privateKey);

                    // Save to SharedPreferences
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(PUBLIC_KEY_KEY, byteArrayToHexString(toByteArray(publicKey)));
                    editor.putString(PRIVATE_KEY_KEY, byteArrayToHexString(toByteArray(privateKey)));
                    editor.putString(CERTIFICATE_KEY, byteArrayToHexString(toByteArray(certificate)));
                    editor.apply();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static X509Certificate getCertificate() {
        return certificate;
    }

    private static X509Certificate generateCertificate(PublicKey publicKey, PrivateKey privateKey) throws Exception {
        KeyPair keyPair = new KeyPair(publicKey, privateKey);

        X500Name issuerName = new X500Name("CN=Test Certificate");
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30),
                new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10)),
                issuerName,
                keyPair.getPublic()
        );

        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = certBuilder.build(contentSigner);

        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    public static boolean verifyDigitalSignature(X509Certificate certificate, PublicKey publicKey) {
        try {
            certificate.verify(publicKey);
            return true;
        } catch (Exception e) {
            Log.e("Demo3", "Digital Signature Verification Failed");
            return false;
        }
    }

    private static byte[] toByteArray(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    private static Object fromByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
