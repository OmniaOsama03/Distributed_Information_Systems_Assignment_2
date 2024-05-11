import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Scanner;


public class Client{
    private static Scanner scanner = new Scanner(System.in);

    public static void main (String args[]) {


        // args[0] = ID
        // args[1] = Server IP "localhost"

        int clientID = Integer.parseInt(args[0]);

        int serverPort = 40000;
        String serverIP = args[1];

        Socket s = null;
        DataInputStream in = null;
        try{
            s = new Socket(serverIP, serverPort);
            in = new DataInputStream(s.getInputStream());
            DataOutputStream  out = new DataOutputStream(s.getOutputStream());

            // Generate AES key with appropriate length
            SecretKeySpec secretKey = SecurityUtil.generateAESKey();
            Cipher cipher = Cipher.getInstance("AES");

            if (!authenticateUser(out, in, cipher, secretKey)) {
                System.out.println("Authentication failed. Exiting...");
                return;
            }

            // Display welcome message
            System.out.println("Welcome to the Event Management System!");

            String upcomingEvents = in.readUTF();
            System.out.println(SecurityUtil.decrypt(upcomingEvents, cipher, secretKey));

            String ActiveEvents = in.readUTF();
            System.out.println(SecurityUtil.decrypt(ActiveEvents, cipher, secretKey));

            // Prompt user for action
            System.out.println("\nPlease enter your action (e.g., join <event_id>):");
            String action = scanner.nextLine();
            String fullMessage = action + " " + clientID;

            String encryptedMessage = SecurityUtil.encrypt(fullMessage, cipher, secretKey);
            out.writeUTF(encryptedMessage);


            while (true) {
                // Read input from the server
                if (in.available() > 0) {
                    String encryptedData = in.readUTF();
                    String decryptedData = SecurityUtil.decrypt(encryptedData, cipher, secretKey);
                    System.out.println(decryptedData);

                    if(!decryptedData.startsWith("---") && !decryptedData.startsWith("\n---")){
                        String output = scanner.nextLine();

                        encryptedMessage = SecurityUtil.encrypt(output, cipher, secretKey);
                        out.writeUTF(encryptedMessage);
                    }
                }
            }

        }catch (UnknownHostException e) {System.out.println("Error Socket:"+e.getMessage());
        }catch (IOException e){
            System.out.println("Exception: " + e.getMessage()); e.printStackTrace();}
        catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    System.out.println("Error closing object input stream: " + e.getMessage());
                }
            }
        }
    }

    private static boolean authenticateUser(DataOutputStream out, DataInputStream in, Cipher cipher, SecretKeySpec secretKey) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        System.out.println("Enter your credentials to authenticate:");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        String encryptedUsername = SecurityUtil.encrypt(username, cipher, secretKey);
        String encryptedPassword = SecurityUtil.encrypt(password, cipher, secretKey);

        out.writeUTF(encryptedUsername);
        out.writeUTF(encryptedPassword);

        String authenticationResponse = in.readUTF();
        return authenticationResponse.equals("authenticated");
    }
}

