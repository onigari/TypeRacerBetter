package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server {
    public static final int MAX_PLAYERS = 4;
    public static final int COUNTDOWN_SEC = 15;
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final String[] PARAGRAPHS = {
            "The quick brown fox jumps over the lazy dog.",
            "Programming is the art of telling another human what one wants the computer to do.",
            "Java is to JavaScript what car is to carpet.",
            "The best way to predict the future is to invent it."
    };
    private static final String selectedParagraph = PARAGRAPHS[new Random().nextInt(PARAGRAPHS.length)];

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            System.out.println("Selected paragraph: " + selectedParagraph);

            while (true) {
                Socket socket = serverSocket.accept();
                if (clients.size() >= MAX_PLAYERS) {
                    System.out.println("Max players reached, rejecting connection: " + socket);
                    socket.close();
                    continue;
                }
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, clients, selectedParagraph);
                synchronized (clients) {
                    clients.add(handler);
                }
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcastStartGame() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("START_GAME");
            }
        }
    }
}