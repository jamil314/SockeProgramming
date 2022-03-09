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
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;



public class ClientHandler implements Runnable, Subscriber {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<ClientHandler>();
    public static Notifier notifier = new Notifier();
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String clientUserName;
    private String incomingFolder;
    private FileOutputStream fos;

    ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            clientUserName = in.readUTF();
            this.incomingFolder ="files/"+clientUserName;
            clientHandlers.add(this);
            notifier.subscribe(this);
            // notifier.broadCast("[SERVER]: New user " + clientUserName + " joined the chat room");
            broadcastMessage("[SERVER]: New user " + clientUserName + " joined the chat room");
        } catch (Exception e){
            e.printStackTrace();
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

    // public void broadcastMessage(String message){
    //     for(ClientHandler clientHandler : clientHandlers){
    //         try {
    //             if(!clientHandler.equals(this)){
    //                 clientHandler.out.writeUTF(message + "\n");
    //                 clientHandler.out.flush();
    //             }
    //         }  catch (IOException e){
    //             closeEverything(socket, in, out);
    //         }
    //     }
    // }

    public void broadcastMessage(String message){
        notifier.unSubscribe(this);
        notifier.broadCast(message);
        notifier.subscribe(this);
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
            notifier.unSubscribe(this);
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
            case "/change_incoming_folder":
                changeIncomingFolder(tokens[1]);
                break;
            default:
                invalidCommand();
                break;
        }
    }


    private void send(String receiver, String fileName){
        try {
            for(ClientHandler clientHandler : clientHandlers){
                if(!clientHandler.clientUserName.equals(clientUserName) &&
                    (receiver.equals("/all") || receiver.equals(clientHandler.clientUserName))){ 
                    System.out.println("sending <" + fileName + "> to <" + receiver+">  from <"+this.clientUserName+">");
                    String targetFolder = clientHandler.incomingFolder;
                    File file = new File(targetFolder +"/" + fileName);
                    String extention = fileName.substring(fileName.lastIndexOf("."));
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                    int multi=0;
                    while(file.exists()){
                        multi++;
                        file = new File(targetFolder + "/" + fileName + "(" + multi + ")"+extention);
                    }
                    if(multi>0) fileName = fileName+"("+multi+")"+extention;
                    else fileName = fileName+extention;
                    clientHandler.fos = new FileOutputStream(targetFolder + "/" + fileName);
                }
            }
            try {
                long fileLength = in.readLong();
                long total = 0;
                while(fileLength>0){
                    int chunkSize = in.readInt();
                    total+=chunkSize;
                    System.out.println(chunkSize+" bytes of data transfered, in total: "+total);
                    byte[] buffer = new byte[chunkSize];
                    in.read(buffer, 0, chunkSize);

                    for(ClientHandler clientHandler : clientHandlers){
                        if(!clientHandler.clientUserName.equals(clientUserName) &&
                        (receiver.equals("/all") || receiver.equals(clientHandler.clientUserName))){
                            clientHandler.fos.write(buffer, 0, chunkSize);
                        }
                    }
                    fileLength -= chunkSize;
                }
                for(ClientHandler clientHandler : clientHandlers){
                    if(!clientHandler.clientUserName.equals(clientUserName) &&
                    (receiver.equals("/all") || receiver.equals(clientHandler.clientUserName))){
                        clientHandler.fos.flush();
                        clientHandler.fos.close();
                        sendPrivateMessage(clientHandler.clientUserName, "[SERVER]: "+clientUserName+" sent you a file: "+fileName);

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }




        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void invalidCommand() {
        try {
            out.writeUTF("[SERVER]: Invalid command !!!  Type /help to see all the valid commands" + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    

    private void listFiles() {
        
        File file = new File(incomingFolder); 
        String[] files = file.list();
        String reply = "Incoming Folder: "+ incomingFolder + "\n";
        for(String fileName : files){
            reply+= fileName + "\n";
        }

        try {
            out.writeUTF(reply + "____________\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
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

    private void listUsers() throws IOException{
        String users = "";
        for(ClientHandler clientHandler : clientHandlers){
            users += clientHandler.clientUserName + "\n";
        }
        users+="__________";
        out.writeUTF(users + "\n");
        out.flush();
    }

    private void changeIncomingFolder(String newIncomingFolder){
        this.incomingFolder = newIncomingFolder;
        File file = new File(newIncomingFolder);
            if(!file.exists())
                file.mkdirs();
    }


    private void help() {
        try {
            out.writeUTF("/list_users - list of users in the chat room\n" +
                    "/whisper <username> <message> - send private message\n" +
                    "/help - list of commands\n" +
                    "/list_files - list of files in your folder\n" +
                    "/change_incoming_folder <filePath> - change your download location to [filePath]"+
                    "/send /all <file_name> - send file to everyone\n" +
                    "/send <username> <file_name> - send file to user named [username]\n"+
                    "/biday_prithibi - exit the chat room\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void onNext(Object item) {
        try {
            out.writeUTF(item.toString());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }


    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();        
    }


    @Override
    public void onComplete() {
        // TODO Auto-generated method stub
        
    }
}
