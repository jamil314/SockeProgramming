import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

class SubscriberAndName{
    Subscriber<Message> subscriber;
    String name;
    SubscriberAndName(Subscriber<Message> subscriber, String name){
        this.subscriber = subscriber;
        this.name = name;
    }
}

public class Server implements Publisher<Message> {

    String tName;
    ArrayList<SubscriberAndName> subscribers = new ArrayList<SubscriberAndName>();
    @Override
    public void subscribe(Subscriber<? super Message> subscriber) {
        Subscriber<Message> sub = (Subscriber<Message>) subscriber;
        subscribers.add(new SubscriberAndName(sub, tName));
    }
    public void unSubscribe(Subscriber<Message> subscriber){
        //  subscribers.remove(new SubscriberAndName(subscriber, tName));
        System.out.println("Removing subscriber: "+tName);
        for(SubscriberAndName subscriberAndName: subscribers ){
            if(subscriberAndName.name.equals(tName)){
                subscribers.remove(subscriberAndName);
                break;
            }
        }
    }
    public void broadCast(Message message) {
        // for (Subscriber<Message> subscriber : subscribers) {
        //     subscriber.onNext(message);
        // }
        for(SubscriberAndName subscriberAndName: subscribers ){
            if(validate(message.sender, message.receiver, subscriberAndName.name))
                subscriberAndName.subscriber.onNext(message);
        }
    }
    private boolean validate(String sender, String receiver, String clientUserName) {
        if(sender.equals(clientUserName)) return false;
        if(receiver.equals(clientUserName) || receiver.equals("/all")) return true;
        return false;
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
