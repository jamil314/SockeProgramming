import java.util.ArrayList;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public class Notifier implements Publisher{
    ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
    @Override
    public void subscribe(Subscriber subscriber) {
        subscribers.add(subscriber);
    }
    public void unSubscribe(Subscriber subscriber){
        subscribers.remove(subscriber);
    }
    public void broadCast(String string) {
        for (Subscriber subscriber : subscribers) {
            subscriber.onNext(string);
        }
    }
    
}
