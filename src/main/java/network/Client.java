package network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private Consumer<String> onMessageReceived;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        listen();
    }

    private void listen() {
        Thread listener = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listener.setDaemon(true);
        listener.start();
    }

    public void setOnMessageReceived(Consumer<String> consumer) {
        this.onMessageReceived = consumer;
    }

    public void sendName(String name) {
        out.println("NAME:" + name);
    }

    public void sendResult(String result) {
        out.println("RESULT:" + result);
    }

    public void sendGameStart() {
        out.println("GAME-START");
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
