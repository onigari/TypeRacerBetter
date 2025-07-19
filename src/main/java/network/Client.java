package network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<String> onMessageReceived;
    private volatile boolean running = true;

    public Client(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try {
                    String msg;
                    while (running && (msg = in.readLine()) != null) {
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(msg);
                        }
                    }
                } catch (IOException e) {
                    if (running && onMessageReceived != null) {
                        onMessageReceived.accept("ERROR: Workshop disconnected from server");
                    }
                } finally {
                    close();
                }
            }).start();
        } catch (IOException e) {
            close();
            throw new IOException("Failed to connect to server: " + e.getMessage());
        }
    }

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    public void sendName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            out.println("NAME:" + name.trim());
        }
    }

    public void sendResult(String result) {
        if (result != null && !result.isEmpty()) {
            out.println("RESULT:" + result);
        }
    }

    public void sendStartGame() {
        out.println("START_GAME");
    }

    public void close() {
        running = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }

    public void sendProgress(double progress) {
        if (progress >= 0 && progress <= 1) {
            out.println("PROGRESS:" + progress);
        }
    }

    public void sendDebugMessage(String s) {
        out.println(s);
    }

    public void sendMessage(String s) {
        out.println(s);
    }
}