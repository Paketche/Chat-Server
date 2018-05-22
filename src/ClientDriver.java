import com.company.MessageFactory;
import com.company.SimpleMessage;
import com.company.client.Client;

import java.net.InetSocketAddress;

public class ClientDriver {
    public static void main(String[] args) {

        MessageFactory factory = new SimpleMessage();
        Client client = new Client(new InetSocketAddress("localhost", 8085), factory);

        Thread clientThread = new Thread(client);


        clientThread.start();
        try {
            clientThread.join();
        } catch (InterruptedException e) {
            System.out.println("Sorry something happened: " + e.getMessage());
        }

        System.out.println("Bye.");
    }
}
