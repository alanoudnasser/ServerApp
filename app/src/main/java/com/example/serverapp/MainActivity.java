package com.example.serverapp;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;


import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    Thread serverThread = null;
    public static final int SERVER_PORT = 33768;
    private LinearLayout msgList;
    private Handler handler;
    private int greenColor;
    private EditText edMessage;
    String msg;
    String dec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Server");
        greenColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        msg = "Hello";
        try {
            // Generate certificate and keys only once
            AsymmetricEncryptionUtils.generateCertificateAndKeys(getApplicationContext());

            // Get the public key and certificate
            PublicKey publicKey = AsymmetricEncryptionUtils.getPublicKey();
            X509Certificate certificate = AsymmetricEncryptionUtils.getCertificate();

            if (publicKey != null && certificate != null) {
                // Log the public key in Base64 encoding
                //byte[] publicKeyBytes = publicKey.getEncoded();
                //String base64PublicKey = Base64.encodeToString(publicKeyBytes, Base64.DEFAULT);
                Log.d("PublicKey", "Public Key : " + publicKey.toString());

                // Base64 encoded encrypted message
                //String base64EncryptedMessage = "h6gmRV5IDfqK4a+eVgHSp1Sl3QRjH20j8QQnkTvgqUdjJ3fQNOw6d20GI/CS3HZjGriUbt16rPu/33iKKiV9XQ==";

                // Decode the base64-encoded string to get the encrypted message bytes
              //  byte[] encryptedMessage = Base64.decode(base64EncryptedMessage, Base64.DEFAULT);

                // Initialize the Cipher object for decryption
                //Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
               // cipher.init(Cipher.DECRYPT_MODE, AsymmetricEncryptionUtils.getPrivateKey());

                // Decrypt the ciphertext
               // byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

                // Convert decrypted bytes to a String
              //  String decryptedMessage = new String(decryptedBytes, "UTF8");
                //Log.d("DecryptedMessage", decryptedMessage);
                if (AsymmetricEncryptionUtils.verifyDigitalSignature(certificate, publicKey)) {
                    Log.d("Demo3", "Digital Signature Verified");
                    // Step 12: Print the certificate
                    String certificateString = certificate.toString();
                    Log.d("Demo3", "Certificate:\n" + certificateString);
                } else {
                    Log.d("Demo3", "Digital Signature Verification Failed");
                }

            } else {
                Log.e("Demo3", "Failed to obtain public key or certificate");
            }

        } catch (Exception e) {
            Log.e("Exception", "Error occurred", e);
        }
    }

    public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color));
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_server) {
            msgList.removeAllViews();
            showMessage("Server Started.", Color.BLACK);
            this.serverThread = new Thread(new ServerThread());
            this.serverThread.start();
            return;
        }
        if (view.getId() == R.id.send_data) {
            String msg = edMessage.getText().toString().trim();
            showMessage("Server : " + msg, Color.BLUE);
            sendMessage(msg);
        }
    }

    private void sendMessage(final String message) {
        try {
            if (null != tempClientSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(message);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                findViewById(R.id.start_server).setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
            }
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                    }
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED);
            }
            showMessage("Connected to Client!!", greenColor);
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    StringBuilder messageBuilder = new StringBuilder();
                    String line;
                    // Check if there is more data to read
                    while (input.ready()) {
                        line = input.readLine();
                        if (line == null || "Disconnect".equals(line)) {
                            // If the line is null or 'Disconnect', handle disconnection
                            Thread.currentThread().interrupt();
                            messageBuilder.append("Client Disconnected");
                            showMessage("Client : Client Disconnected", greenColor);
                            break;
                        }
                        messageBuilder.append(line).append("\n");
                    }

                    // Check if we built a message to process
                    String read = messageBuilder.toString();
                    if (!read.isEmpty()) {
                        showMessage("Client : " + read, greenColor);
                        Log.e("Demo6", read);
                        dec(read);
                    }

                    // Add a small delay to allow for data to accumulate
                    try {
                        Thread.sleep(100); // Sleep for a short time (e.g., 100 ms)
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore the interrupted status
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        }



    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }
    private void dec(String msg){
        try{
            byte[] encryptedMessage = Base64.decode(msg, Base64.DEFAULT);

            // Initialize the Cipher object for decryption
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, AsymmetricEncryptionUtils.getPrivateKey());

            // Decrypt the ciphertext
            byte[] decryptedBytes = cipher.doFinal(encryptedMessage);

            // Convert decrypted bytes to a String
            String decryptedMessage = new String(decryptedBytes, "UTF8");
            Log.e("Demo7", decryptedMessage);

        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
}