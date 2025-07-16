package network;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final List<ClientHandler> clients;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName = "";

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // First connected client becomes the host
            if (clients.size() == 1) {
                sendMessage("HOST:"); // Will be overridden once name is received
            }

            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("NAME:")) {
                    playerName = input.substring(5);
                    Server.broadcastPlayerList();
                    if (clients.get(0) == this) {
                        sendMessage("HOST:" + playerName);
                    }
                } else if (input.equals("GAME-START")) {
                    if (clients.get(0) == this && clients.size() >= 2) {
                        Server.setGameStarted(true);
                        Server.broadcast("GAME-START");
                    } else {
                        sendMessage("ERROR:Only the host can start or not enough players.");
                    }
                } else if (input.startsWith("RESULT:")) {
                    Server.broadcast(input); // broadcast score
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + playerName);
        } finally {
            try {
                clients.remove(this);
                Server.broadcastPlayerList();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
