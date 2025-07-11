package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final String PARAGRAPH = "The quick brown fox jumps over the lazy dog.";

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("TypeRacer Server started on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("New client connected: " + socket);
            ClientHandler handler = new ClientHandler(socket, clients, PARAGRAPH);
            clients.add(handler);
            new Thread(handler).start();
        }
    }
}

