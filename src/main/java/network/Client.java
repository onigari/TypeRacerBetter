package network;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Consumer<String> onMessageReceived;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(() -> {
            String msg;
            try {
                while ((msg = in.readLine()) != null) {
                    if (onMessageReceived != null) {
                        onMessageReceived.accept(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    public void sendName(String name) {
        out.println("NAME:" + name);
    }

    public void sendResult(String result) {
        out.println("RESULT:" + result);
    }

    public void close() throws IOException {
        socket.close();
    }
}

