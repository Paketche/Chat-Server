package valchanov.georgi.drivers;

import valchanov.georgi.client.Client;
import valchanov.georgi.messages.MessageFactory;
import valchanov.georgi.messages.SimpleMessage;

import java.net.InetSocketAddress;

public class ClientDriver {
    /**
     * starts a new Client instance. The arguments of the function are as followed:
     * <ol>
     * <li>server address</li>
     * <li>server port</li>
     * </ol>
     *
     * @param args to set up the client
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            ClientDriver.usage();
        }

        try {
            MessageFactory factory = new SimpleMessage();
            Client client = new Client(new InetSocketAddress(args[0], Integer.parseInt(args[1])), factory);

            Thread clientThread = new Thread(client);


            clientThread.start();
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                System.out.println("Sorry something happened: " + e.getMessage());
            }

            System.out.println("Bye.");
        } catch (NumberFormatException e) {
            usage();
        } catch (Exception e) {
            System.out.println("Fatal exception occurred: " + e.getMessage());
        }
    }

    private static void usage() {
    }
}
