package network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Server {
    public static final int MAX_PLAYERS = 4;
    public static final int COUNTDOWN_SEC = 15;
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static List<String> inputStrings = new ArrayList<>() ;
    private static String selectedParagraph; //= PARAGRAPHS[new Random().nextInt(PARAGRAPHS.length)];

    private static void selectParagraph() {
        try {
            File file = new File("src/main/resources/txtFiles/input.txt");
            Scanner takeIn = new Scanner(file);
            while (takeIn.hasNextLine()) {
                inputStrings.add(takeIn.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        selectedParagraph = inputStrings.get(new Random().nextInt(inputStrings.size()));
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            selectParagraph();

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
            try{
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (ClientHandler client : clients) {
                client.sendMessage("PARAGRAPH:" + selectedParagraph);
            }
        }
    }
}