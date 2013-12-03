package com.nickilous.orderupclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.nickilous.OrderMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Nick on 10/7/13.
 */
public class OrderUpConnection {
    private Handler mUpdateHandler;
    private OrderUpServer mOrderUpServer;
    private OrderUpClient mOrderUpClient;

    private static String ADD_ORDER;
    private static String REMOVE_ORDER;

    private static final String TAG = "OrderUpConnection";

    private Socket mSocket;
    private int mPort = -1;


    public OrderUpConnection(Handler handler) {
        mUpdateHandler = handler;

    }

    public void tearDown() {
        if(mOrderUpServer != null) mOrderUpServer.tearDown();
        if(mOrderUpClient != null) mOrderUpClient.tearDown();
    }

    public void connectToServer(InetAddress address, int port) {
        mOrderUpClient = new OrderUpClient(address, port);
    }

    public void sendMessage(OrderMessage msg) {
        if (mOrderUpClient != null) {
            mOrderUpClient.sendMessage(msg);
        }
    }

    public int getLocalPort() {
        return mPort;
    }

    public void setLocalPort(int port) {
        mPort = port;
    }



    public synchronized void updateMessages(OrderMessage msg) {
        Log.e(TAG, "Updating message: " + msg);

        Bundle messageBundle = new Bundle();
        messageBundle.putSerializable("orderMessage", msg);

        Message message = new Message();
        message.setData(messageBundle);
        mUpdateHandler.sendMessage(message);

    }

    private synchronized void setSocket(Socket socket) {
        Log.d(TAG, "setSocket being called.");
        if (socket == null) {
            Log.d(TAG, "Setting a null socket.");
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = socket;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public boolean isConnected(){
        if((mSocket != null) && mSocket.isConnected()){
            return true;
        }else{
            return false;
        }
    }


    private class OrderUpServer {
        ServerSocket mServerSocket = null;
        Thread mThread = null;

        public OrderUpServer(Handler handler) {
            mThread = new Thread(new ServerThread());
            mThread.start();
        }

        public void tearDown() {
            mThread.interrupt();
            try {
                mServerSocket.close();
            } catch (IOException ioe) {
                Log.e(TAG, "Error when closing server socket.");
            }
        }

        class ServerThread implements Runnable {

            @Override
            public void run() {

                try {

                    mServerSocket = new ServerSocket(0);
                    setLocalPort(mServerSocket.getLocalPort());

                    while (!Thread.currentThread().isInterrupted()) {
                        Log.d(TAG, "ServerSocket Created, awaiting connection");
                        setSocket(mServerSocket.accept());
                        Log.d(TAG, "Connected.");
                        if (mOrderUpClient == null) {
                            int port = mSocket.getPort();
                            InetAddress address = mSocket.getInetAddress();
                            connectToServer(address, port);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error creating ServerSocket: ", e);
                    e.printStackTrace();
                }
            }
        }
    }

    private class OrderUpClient {

        private InetAddress mAddress;
        private int PORT;

        private final String CLIENT_TAG = "OrderUpClient";

        private Thread mSendThread;
        private Thread mRecThread;

        public OrderUpClient(InetAddress address, int port) {

            Log.d(CLIENT_TAG, "Creating OrderUpClient");
            this.mAddress = address;
            this.PORT = port;

            mSendThread = new Thread(new SendingThread());
            mSendThread.start();
        }

        class SendingThread implements Runnable {

            ArrayBlockingQueue<OrderMessage> mMessageQueue;
            private int QUEUE_CAPACITY = 10;

            public SendingThread() {
                mMessageQueue = new ArrayBlockingQueue<OrderMessage>(QUEUE_CAPACITY);
            }

            @Override
            public void run() {
                try {
                    if (getSocket() == null) {
                        setSocket(new Socket(mAddress, PORT));
                        Log.d(CLIENT_TAG, "Client-side socket initialized.");

                    } else {
                        Log.d(CLIENT_TAG, "Socket already initialized. skipping!");
                    }

                    mRecThread = new Thread(new ReceivingThread());
                    mRecThread.start();

                } catch (UnknownHostException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, UHE", e);
                } catch (IOException e) {
                    Log.d(CLIENT_TAG, "Initializing socket failed, IOE.", e);
                }

                while (true) {
                    try {
                        OrderMessage msg = mMessageQueue.take();
                        sendMessage(msg);
                    } catch (InterruptedException ie) {
                        Log.d(CLIENT_TAG, "Message sending loop interrupted, exiting");
                    }
                }
            }
        }

        class ReceivingThread implements Runnable {
            OrderMessage mOrderMessage;
            @Override
            public void run() {

                try {
                    ObjectInputStream inputStream = new ObjectInputStream(mSocket.getInputStream());

                    try {
                        mOrderMessage = (OrderMessage) inputStream.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    while (!Thread.currentThread().isInterrupted()) {


                        if (mOrderMessage != null) {
                            Log.d(CLIENT_TAG, "Read from the stream: " + mOrderMessage.toString());
                            updateMessages(mOrderMessage);
                        } else {
                            Log.d(CLIENT_TAG, "The nulls! The nulls!");
                            break;
                        }
                    }
                    inputStream.close();

                } catch (IOException e) {
                    Log.e(CLIENT_TAG, "Server loop error: ", e);
                }
            }
        }

        public void tearDown() {
            try {
                getSocket().close();
            } catch (IOException ioe) {
                Log.e(CLIENT_TAG, "Error when closing server socket.");
            }
        }

        public void sendMessage(OrderMessage msg) {

            try {
                Socket socket = getSocket();
                if (socket == null) {
                    Log.d(CLIENT_TAG, "Socket is null, wtf?");
                } else if (socket.getOutputStream() == null) {
                    Log.d(CLIENT_TAG, "Socket output stream is null, wtf?");
                }
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(msg);
                outputStream.flush();

                //updateMessages(msg, true);
            } catch (UnknownHostException e) {
                Log.d(CLIENT_TAG, "Unknown Host", e);
            } catch (IOException e) {
                Log.d(CLIENT_TAG, "I/O Exception", e);
            } catch (Exception e) {
                Log.d(CLIENT_TAG, "Error3", e);
            }
            Log.d(CLIENT_TAG, "Client sent message: " + msg);
        }
    }
}