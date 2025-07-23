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
    //private String paragraph;
    private String playerName;
    private static final CopyOnWriteArrayList<String> leaderboard = new CopyOnWriteArrayList<>();
    private double currentProgress = 0;

    public ClientHandler(Socket socket, List<ClientHandler> clients, String paragraph) throws IOException {
        this.socket = socket;
        this.clients = clients;
        //this.paragraph = paragraph;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("ClientHandler created for: " + socket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from " + (playerName != null ? playerName : "unknown") + ": " + inputLine);
                if (inputLine.startsWith("NAME:")) {
                    this.playerName = inputLine.substring(5).trim();
                    if (playerName.isEmpty()) playerName = "Anonymous_" + socket.getPort();
                    broadcastPlayerList();
                } else if (inputLine.startsWith("RESULT:")) {
                    handleResult(inputLine.substring(7));
                } else if (inputLine.startsWith("PROGRESS:")) {
                    handleProgress(inputLine.substring(9));
                } else if (inputLine.equals("START_GAME") && clients.get(0) == this) {
                    System.out.println("Host (" + playerName + ") initiated game start");
                    Server.broadcastStartGame();
                    broadcastPlayerList();
                } else if (inputLine.equals("CLOSE")){
                    break;
                } else if (inputLine.equals("CLOSE_ALL")){
                    for (ClientHandler client : clients) {
                        client.out.println("CLOSE_ALL");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + (playerName != null ? playerName : socket.getRemoteSocketAddress()));
        } finally {
            cleanup();
        }
    }

    // dynamic progress bar
    // Add to handle progress messages
    private void handleProgress(String progressData) {
        try {
            double progress = Double.parseDouble(progressData);
            String entry = String.format("%s;%.2f", playerName, progress);
            synchronized (leaderboard) {
                updateProgressEntry(entry);
                broadcastProgress();
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid progress format: " + progressData);
        }
    }

    private void updateProgressEntry(String entry) {
        String[] parts = entry.split(";");
        String player = parts[0];

        for (int i = 0; i < leaderboard.size(); i++) {
            String lbEntry = leaderboard.get(i);
            if (lbEntry.startsWith(player + ";")) {
                leaderboard.set(i, entry);
                return;
            }
        }
        leaderboard.add(entry);
    }

    private void broadcastProgress() {
        StringBuilder sb = new StringBuilder("PROGRESS:");
        synchronized (leaderboard) {
            for (String entry : leaderboard) {
                // Check if this is a result entry (has 3 parts) or progress entry (2 parts)
                String[] parts = entry.split(";");
                if (parts.length == 2) {
                    sb.append(entry).append("|");
                } else if (parts.length == 3) {
                    // For finished players, progress is 1.0
                    sb.append(parts[0]).append(";1.0|");
                }
            }

        }
        String progressData = sb.toString();
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(progressData);
            }
        }
    }

    private boolean checkClient(String name) {
        for (String client : leaderboard) {
            String[] clientParts = client.split(";");
            if (clientParts[0].equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void updateResult(String result) {
        String[] parts = result.split(";");
        for (String client : leaderboard) {
            String[] clientParts = client.split(";");
            if(clientParts[0].equals(parts[0])) {
                leaderboard.remove(client);
                leaderboard.add(result);
                return;
            }
        }
    }

    private void handleResult(String result) {
        String[] parts = result.split(";");
        if (parts.length != 4) {
            System.out.println("Invalid result format: " + result);
            return;
        }
        try {
            String entry = String.format("%s;%s;%s;%s", parts[0], parts[1], parts[2], parts[3]);
            synchronized (leaderboard) {
                if(checkClient(parts[0])) updateResult(entry);
                else leaderboard.add(entry);
                leaderboard.sort((a, b) -> {
                    try {
                        double t1 = Double.parseDouble(a.split(";")[1]);
                        double t2 = Double.parseDouble(b.split(";")[1]);
                        int returnTime = Double.compare(t1, t2);
                        if (Math.abs(t1 - t2) <= 0.3) {
                            double t3 = Double.parseDouble(a.split(";")[2]);
                            double t4 = Double.parseDouble(b.split(";")[2]);
                            int returnWPM = Double.compare(t4, t3);
                            if (returnWPM == 0) {
                                double t5 = Double.parseDouble(a.split(";")[3]);
                                double t6 = Double.parseDouble(b.split(";")[3]);
                                int returnAcc = Double.compare(t6, t5);
                                if(returnAcc == 0) {
                                    return a.split(";")[0].compareTo(b.split(";")[0]);
                                }
                            }
                            return returnWPM;
                        }
                        return returnTime;
                    } catch (Exception e) {
                        return 0;
                    }
                });
                Server.broadcastLeaderboard(leaderboard);
            }
        } catch (Exception e) {
            System.err.println("Error processing result: " + e.getMessage());
        }
    }

//    private void broadcastLeaderboard() {
//        StringBuffer sb = new StringBuffer("LEADERBOARD:");
//        synchronized (leaderboard) {
//            for (String entry : leaderboard) {
//                String[] parts = entry.split(";");
//                if (parts.length == 3) {
//                    sb.append(String.format("%s - %.2fs - %.2f WPM)|",
//                            parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
//                }
//            }
//        }
//        synchronized (clients) {
//            for (ClientHandler client : clients) {
//                client.sendMessage(sb.toString());
//            }
//        }
//    }

    private void broadcastPlayerList() {
        if(clients.isEmpty()) return;
        StringBuffer sb = new StringBuffer("PLAYERS:");
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
            //out.println("CLOSE:" + playerName);
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