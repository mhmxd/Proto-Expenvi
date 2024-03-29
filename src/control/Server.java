package control;

import tools.Logs;
import tools.Memo;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private final static String NAME = "Server/";

    private static Server instance; // Singelton

    private final int PORT = 8000; // always the same
    private final int CONNECTION_TIMEOUT = 60 * 1000; // 1 min

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter outPW;
    private BufferedReader inBR;

    private final ExecutorService executor;

    //----------------------------------------------------------------------------------------

    //-- Runnable for waiting for incoming connections
    private class ConnWaitRunnable implements Runnable {
        String TAG = NAME + "ConnWaitRunnable";

        @Override
        public void run() {
            try {
                Logs.d(TAG, "Waiting for connections...");
                if (serverSocket == null) serverSocket = new ServerSocket(PORT);
                socket = serverSocket.accept();

                // When reached here, Moose is connected
                Logs.d(TAG, "Moose connected!");

                // Create streams
                inBR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outPW = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())), true);

                // Start receiving
                executor.execute(new InRunnable());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //-- Runnable for sending messages to Moose
    private class OutRunnable implements Runnable {
        String TAG = NAME + "OutRunnable";
        Memo message;

        public OutRunnable(Memo mssg) {
            message = mssg;
        }

        @Override
        public void run() {
            if (message != null && outPW != null) {
                outPW.println(message);
                outPW.flush();
            }
        }
    }

    //-- Runnable for receiving messages from Moose
    private class InRunnable implements Runnable {
        String TAG = NAME + "InRunnable";

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted() && inBR != null) {
                try {
                    Logs.d(TAG, "Reading messages...");
                    String message = inBR.readLine();
                    Logs.d(TAG, "Message: " + message);
                    if (message != null) {
                        Memo memo = Memo.valueOf(message);

                        // Check the action...
                        if (memo.getAction().equals("")) {

                        }

                    } else {
                        Logs.d(TAG, "Moose disconnected.");
                        start();
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Error in reading from Moose");
                    start();
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------

    /**
     * Get the instance
     * @return single instance
     */
    public static Server get() {
        if (instance == null) instance = new Server();
        return instance;
    }

    /**
     * Constructor
     */
    public Server() {
        String TAG = NAME;

        // Init executerService for running threads
        executor = Executors.newCachedThreadPool();
    }

    /**
     * Start the server
     */
    public void start() {
        String TAG = NAME + "start";

        executor.execute(new ConnWaitRunnable());
    }

    /**
     * Send a Memo to the Moose
     * Called from outside
     * @param mssg Memo message
     */
    public void send(Memo mssg) {
        executor.execute(new OutRunnable(mssg));
    }

}
