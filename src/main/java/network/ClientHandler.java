package network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private List<ClientHandler> clients;
    private String paragraph;
    private String playerName;

    private static final List<String> leaderboard = new ArrayList<>(); //Static cause shared across all Client

    public ClientHandler(Socket socket, List<ClientHandler> clients, String paragraph) throws IOException {
        this.socket = socket;
        this.clients = clients;
        this.paragraph = paragraph;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void run() {
        try {
            out.println("PARAGRAPH:" + paragraph);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);

                if (inputLine.startsWith("NAME:")) {
                    this.playerName = inputLine.substring(5);
                    continue;
                }

                if (inputLine.startsWith("RESULT:")) {
                    synchronized (leaderboard) {
                        leaderboard.add(inputLine.substring(7));
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
                }
            }
        } catch (IOException e) {
            System.out.println("Connection dropped: " + socket);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            clients.remove(this);
        }
    }

    private void broadcastLeaderboard() {
        for (ClientHandler client : clients) {
            for (String entry : leaderboard) {
                client.out.println("RESULT:" + entry);
            }
        }
    }
}
