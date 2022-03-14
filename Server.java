import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public class Server implements Publisher<String> {


    ArrayList<Subscriber<String>> subscribers = new ArrayList<Subscriber<String>>();
    @Override
    public void subscribe(Subscriber<? super String> subscriber) {
        subscribers.add((Subscriber<String>) subscriber);
        
    }
    public void unSubscribe(Subscriber<String> subscriber){
        subscribers.remove(subscriber);
    }
    public void broadCast(String string) {
        for (Subscriber<String> subscriber : subscribers) {
            subscriber.onNext(string);
        }
    }



    private ServerSocket serverSocket;
    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }
    public void startServer(){
        ClientHandler.server = this;
        try{
            while(!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("New client has connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void closeServerSocket(){
        try{
            if(serverSocket != null){
                serverSocket.close();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        try{
            ServerSocket serverSocket = new ServerSocket(8080);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
