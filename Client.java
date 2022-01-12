import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private String userName;
    private DataInputStream in;
    private DataOutputStream out;

    public Client(Socket socket, String userName) {
        this.socket = socket;
        this.userName = userName;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(userName);
            out.flush();
        } catch (IOException e) {
            closeEverything(socket, in, out);
        }
    }

    public void sendMessage() {
        try {
            try (Scanner scanner = new Scanner(System.in)) {
                while(socket.isConnected()){
                    String msg = scanner.nextLine();
                    String[] tokens = msg.split(" ");
                    switch (tokens[0]) {
                        case "/biday_prithibi":
                            closeEverything(socket, in, out);
                            break;
                        case "/send":
                            if(tokens.length<3){
                                System.out.println("Invalid command!!!\n"+
                                "Usage: /send <username> <path>\n"+
                                "Example: /send jamil C:\\Users\\user\\test.txt\n"+
                                "or: /send /all C:\\Users\\user\\test.txt");
                                break;
                            }
                            File file = new File(tokens[2]);
                            if(!file.exists()){
                                System.out.println("File not found");
                                break;
                            }
                            System.out.println("Sending file: "+file.getName());
                            FileInputStream fileInputStream = new FileInputStream(file);
                            byte[] buffer = new byte[(int)file.length()];
                            fileInputStream.read(buffer);
                            out.writeUTF("/send " + tokens[1] + " " +file.getName());
                            out.writeInt(buffer.length);
                            out.write(buffer);
                            break;
                        default:
                            out.writeUTF(msg);
                            out.flush();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            closeEverything(socket, in, out);
        }
    }

    public void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    while(socket.isConnected()){
                        message = in.readUTF();
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    closeEverything(socket, in, out);
                }
            }
        }).start();
    }
    
    public void closeEverything(Socket socket, DataInputStream in, DataOutputStream out){
        System.out.println("[biday prithibi] " + userName + " is leaving the chat room");
        try{
            if(in != null){
                in.close();
            }
            if(out != null){
                out.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter your name: ");
            String userName = scanner.nextLine();
            while(invalid(userName)){
                userName = scanner.nextLine();
            }
            System.out.println("Welcome " + userName+"\nType command /help to see the commands");
            try {
                Socket socket = new Socket("localhost", 8080);
                Client client = new Client(socket, userName);
                client.listenForMessage();
                client.sendMessage();
            } catch (IOException e) {
                System.out.println("Could not connect to server");
            }

        }
    }

    private static boolean invalid(String name) {
        for(Character c : name.toCharArray()){
            if(!Character.isLetter(c) && !Character.isDigit(c) && c != '_'){
                System.out.println("Username can only contain letters, numbers and underscore: ");
                return true;
            }
        }
        

        try {
            File file = new File("files/"+name);
            if(file.exists()){
                System.out.println("Username already exists: ");
                return true;
            }
            file.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }



        return false;
    }

}
