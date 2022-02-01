import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String clientUserName;

    ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientUserName = in.readUTF();
            clientHandlers.add(this);
            broadcastMessage("[SERVER]: New user " + clientUserName + " joined the chat room");
        } catch (Exception e){
            System.out.println("Error in client handler constructor");
        }
    }


    @Override
    public void run() {
        String message;
        while(socket.isConnected()){
            try {
                message = in.readUTF();
                if(message.substring(0, 1).equals("/")){
                    execute(message);
                }
                else{
                    broadcastMessage(clientUserName + ": " + message);
                }
            } catch (IOException e){
                System.out.println("Client disconnected :: IOException");
                closeEverything(socket, in, out);
                break;
            }
        }
    }

    public void broadcastMessage(String message){
        for(ClientHandler clientHandler : clientHandlers){
            try {
                if(!clientHandler.equals(this)){
                    clientHandler.out.writeUTF(message + "\n");
                    clientHandler.out.flush();
                }
            }  catch (IOException e){
                closeEverything(socket, in, out);
            }
        }
    }

    public boolean removeClientHandler(){
        Path path = Paths.get("files/" + clientUserName);
        String dltMsg="", dir=path.toFile().getName();
        for (File file: path.toFile().listFiles()) {
            dltMsg+="Deleting file: files/"+dir+"/"+file.getName()+"......\n";
            file.delete();
        }
        dltMsg+="Deleting directory: "+dir+"......\n";
        System.out.println(dltMsg);
        if(path.toFile().exists()){
            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("User " + clientUserName + " is leaving the chat room");
            broadcastMessage("[SERVER]: " + clientUserName + " left the chat room !!!");
            clientHandlers.remove(this);
            return true;
        }
        System.out.println("User " + clientUserName + " has already left the chat room");
        return false;
    }
    public void closeEverything(Socket socket, DataInputStream in, DataOutputStream out){
        if(removeClientHandler()){
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
            } catch (IOException e){
                System.out.println("Error while closing the socket");
            }
        }
        
    }

    

    private void execute(String s){
        String[] tokens = s.split(" ");
        switch (tokens[0]){
            case "/whisper":
                String msg=clientUserName+" (Whispering): ";
                for(int i=2; i<tokens.length; i++) {
                    msg += tokens[i] + " ";
                }
                sendPrivateMessage(tokens[1], msg);
                break;
            case "/list_users":
                try {
                    listUsers();
                } catch (IOException e) {
                    System.out.println("Error while listing users");
                }
                break;
            case "/list_files":
                listFiles();
                break;
            case "/help":
                help();
                break;
            case "/send":
                send(tokens[1], tokens[2]);
                break;
            default:
                invalidCommand();
                break;
        }
    }


    

    private void send(String receiver, String fileName) {
        int fileLength;
        try {
            fileLength = in.readInt();
            byte[] buffer = new byte[fileLength];
            in.readFully(buffer, 0, fileLength);
            if(receiver.equals("/all")){
                for(ClientHandler clientHandler : clientHandlers){
                    if(!clientHandler.clientUserName.equals(this.clientUserName)){
                        sendPrivate(clientHandler.clientUserName, fileName, buffer);
                    }
                }
            } else {
                sendPrivate(receiver, fileName, buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void sendPrivate(String receiver, String fileName, byte[] buffer) {
        System.out.println("sending <" + fileName + "> to <" + receiver+">  from <"+this.clientUserName+">");
        File file = new File("files/" + receiver + "/" + fileName);
        String extention = fileName.substring(fileName.lastIndexOf("."));
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        int multi=0;
        while(file.exists()){
            multi++;
            file = new File("files/" + receiver + "/" + fileName + "(" + multi + ")"+extention);
        }
        if(multi>0) fileName = fileName+"("+multi+")"+extention;
        else fileName = fileName+extention;
        try {
            FileOutputStream fos = new FileOutputStream("files/" + receiver + "/" + fileName);
            fos.write(buffer);
            fos.close();
            sendPrivateMessage(receiver, "[SERVER]: "+clientUserName+" sent you a file: "+fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


   


    private void invalidCommand() {
        try {
            out.writeUTF("[SERVER]: Invalid command !!!  Type /help to see all the valid commands" + "\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("Error in invalid command");
        }
    }

    

    private void listFiles() {
        File file = new File("files/"+clientUserName); 
        String[] files = file.list();
        for(String fileName : files){
            try {
                out.writeUTF(fileName + "\n");
                out.flush();
            } catch (IOException e) {
                System.out.println("Error in listFiles");
            }
        }
    }

   
    private void sendPrivateMessage(String receiver, String message) {
        for(ClientHandler clientHandler : clientHandlers){
            if(clientHandler.clientUserName.equals(receiver)){
                try {
                    clientHandler.out.writeUTF(message + "\n");
                    clientHandler.out.flush();
                } catch (IOException e) {
                    closeEverything(socket, in, out);
                }
            }
        }
    }

    void listUsers() throws IOException{
        String users = "";
        for(ClientHandler clientHandler : clientHandlers){
            users += clientHandler.clientUserName + "\n";
        }
        users+="__________";
        out.writeUTF(users + "\n");
        out.flush();
    }

    private void help() {
        try {
            out.writeUTF("/list_users - list of users in the chat room\n" +
                    "/whisper <username> <message> - send private message\n" +
                    "/help - list of commands\n" +
                    "/list_files - list of files in your folder\n" +
                    "/send /all <file_name> - send file to everyone\n" +
                    "/send <username> <file_name> - send file to user named _username_\n"+
                    "/biday_prithibi - exit the chat room\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("Error in help");
        }
    }
}
