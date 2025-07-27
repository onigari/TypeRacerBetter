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
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.out;

public class Server {
    public static final int MAX_PLAYERS = 4;
    public static final int COUNTDOWN_SEC = 15;
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static List<String> inputStrings = new ArrayList<>() ;
    private static String selectedParagraph; //= PARAGRAPHS[new Random().nextInt(PARAGRAPHS.length)];

    private static void selectParagraph(int gameTime) {
        String filePath = "src/main/resources/txtFiles/input" + gameTime + ".txt";
        try {
            File file = new File(filePath);
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
            out.println("Server started on port " + PORT);
            //selectParagraph();

            out.println("Selected paragraph: " + selectedParagraph);

            while (true) {
                Socket socket = serverSocket.accept();
                if (clients.size() >= MAX_PLAYERS) {
                    out.println("Max players reached, rejecting connection: " + socket);
                    socket.close();
                    continue;
                }
                out.println("New client connected: " + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, clients);
                synchronized (clients) {
                    clients.add(handler);
                }
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void broadcastStartGame(int gameTime) {
        selectParagraph(gameTime);
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("START_GAME");
                client.setParagraph(selectedParagraph);
            }
        }
    }

    public static void broadcastRestart() {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("RESTART");
            }
        }
    }

    public static void broadcastLeaderboard(CopyOnWriteArrayList<String> leaderboard){
        StringBuffer sb = new StringBuffer("LEADERBOARD:");
        int count = 0;
        synchronized (leaderboard) {
            for (String entry : leaderboard) {
                String[] parts = entry.split(";");
                if (parts.length == 4) {
                    count++;
                    sb.append(String.format("%s - %.0fs - %.2f WPM - %.2f %% |",
                            parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
                }
            }
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(sb.toString());
            }
        }

        out.println(count);
        out.println(clients.size());

        if(count == clients.size()){
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.sendMessage("GAME_FINISHED");
                }
            }
        }
    }
}