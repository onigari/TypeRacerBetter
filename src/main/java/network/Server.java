package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static final int PORT = 5000;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static boolean gameStarted = false;

    public static void startNewRoom() throws IOException {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Room open at: " + serverSocket.getInetAddress().getHostAddress());

                while (!gameStarted && clients.size() < 5) {
                    Socket clientSocket = serverSocket.accept();
                    if (gameStarted) {
                        clientSocket.close();
                        continue;
                    }

                    ClientHandler handler = new ClientHandler(clientSocket, clients);
                    clients.add(handler);
                    handler.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public static void setGameStarted(boolean started) {
        gameStarted = started;
    }

    public static boolean isGameStarted() {
        return gameStarted;
    }

    public static void removeClient(ClientHandler handler) {
        clients.remove(handler);
        broadcastPlayerList();
    }

    public static void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("LOBBY-PLAYERS:");
        for (ClientHandler ch : clients) {
            sb.append(ch.getPlayerName()).append(",");
        }
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        broadcast(sb.toString());
    }

    public static List<ClientHandler> getClients() {
        return clients;
    }
}


