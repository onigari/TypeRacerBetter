package network;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private List<ClientHandler> clients;
    private String paragraph;
    private String playerName;
    private static final CopyOnWriteArrayList<String> leaderboard = new CopyOnWriteArrayList<>();

    public ClientHandler(Socket socket, List<ClientHandler> clients, String paragraph) throws IOException {
        this.socket = socket;
        this.clients = clients;
        this.paragraph = paragraph;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("ClientHandler created for: " + socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        try {
            System.out.println("Sent PARAGRAPH to client: " + paragraph);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from " + (playerName != null ? playerName : "unknown") + ": " + inputLine);
                if (inputLine.startsWith("NAME:")) {
                    this.playerName = inputLine.substring(5).trim();
                    if (playerName.isEmpty()) playerName = "Anonymous_" + socket.getPort();
                    broadcastPlayerList();
                } else if (inputLine.startsWith("RESULT:")) {
                    handleResult(inputLine.substring(7));
                } else if (inputLine.equals("START_GAME") && clients.get(0) == this) {
                    System.out.println("Host (" + playerName + ") initiated game start");
                    Server.broadcastStartGame();
                    Server.broadcastParagraph();
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + (playerName != null ? playerName : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    private void handleResult(String result) {
        String[] parts = result.split(";");
        if (parts.length != 3) {
            System.out.println("Invalid result format: " + result);
            return;
        }
        try {
            String entry = String.format("%s;%s;%s", parts[0], parts[1], parts[2]);
            synchronized (leaderboard) {
                leaderboard.add(entry);
                leaderboard.sort((a, b) -> {
                    try {
                        double t1 = Double.parseDouble(a.split(";")[1]);
                        double t2 = Double.parseDouble(b.split(";")[1]);
                        return Double.compare(t1, t2);
                    } catch (Exception e) {
                        return 0;
                    }
                });
                broadcastLeaderboard();
            }
        } catch (Exception e) {
            System.err.println("Error processing result: " + e.getMessage());
        }
    }

    private void broadcastLeaderboard() {
        StringBuilder sb = new StringBuilder("LEADERBOARD:");
        synchronized (leaderboard) {
            for (String entry : leaderboard) {
                String[] parts = entry.split(";");
                if (parts.length == 3) {
                    sb.append(String.format("%s - %.2fs (%.2f WPM)|",
                            parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                }
            }
        }
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(sb.toString());
            }
        }
    }

    private void broadcastPlayerList() {
        StringBuilder sb = new StringBuilder("PLAYERS:");
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.playerName != null) {
                    sb.append(client.playerName).append(",");
                }
            }
        }
        String playerList = sb.toString();
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(playerList);
            }
        }
    }

    void sendMessage(String message) {
        out.println(message);
        System.out.println("Sent to " + (playerName != null ? playerName : socket.getRemoteSocketAddress()) + ": " + message);
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error cleaning up: " + e.getMessage());
        }
        synchronized (clients) {
            clients.remove(this);
        }
        broadcastPlayerList();
    }
}