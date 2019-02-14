package com.example.xxxxxxxx.bluetoothtowifiapp;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Waits for incoming connection on port 5004 (Port same as sender class).
 * When it detects any incoming connection, it will accept it and read the incoming data
 * and store it in the internal storage.
 */
public class Receiver extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "ReceiverClass";
    private static final int PORT = 5004;

    private Context context;
    private Activity activity;
    private boolean xceptionFlag = false;
    private ServerSocket serversocket;
    private Socket socket;
    private Button mServerBtn;


    public Receiver(Context context, Activity activity, Button serverBtn) {
        this.context = context;
        this.activity = activity;
        this.mServerBtn = serverBtn;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        // Listens for connections on PORT, and saves then to the phones storage
        try {
            //this is done instead of above line because it was giving error of address is already in use.
            serversocket = new ServerSocket();
            serversocket.setReuseAddress(true);
            serversocket.bind(new InetSocketAddress(PORT));

            Log.d(TAG, "Listening for a connection");
            socket = serversocket.accept();
            Log.d(TAG, "Connection accepted!");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            //read the number of files from the client
            int number = dis.readInt();
            ArrayList<File> files = new ArrayList<>(number);
            Log.d(TAG, "Number of Files to be received: " + number);

            ArrayList<Long> fileSize = new ArrayList<>(number);

            for (int i = 0; i < number; i++) {
                long size = dis.readLong();
                System.out.println(size);
                fileSize.add(size);
            }

            //read file names, add files to arraylist
            for (int i = 0; i < number; i++) {
                File file = new File(dis.readUTF());
                files.add(file);
            }
            int n = 0;
            byte[] buf = new byte[4092];

            //outer loop, executes one for each file
            for (int i = 0; i < files.size(); i++) {

                Log.d(TAG, "Receiving file: " + files.get(i).getName());

                // TODO: This is where the file path and name is declared
                //Create new Folder for our app, if it is not there and store received files there in our separate folder.
                File folder = new File(Environment.getExternalStorageDirectory() +
                        File.separator + "File");
                boolean fileExists = false;
                if (!folder.exists()) {
                    fileExists = folder.mkdirs();
                }
                if (fileExists) {
                    // Do something on success
                } else {
                    // Do something else on failure
                }

                //create a new fileoutputstream for each new file
                FileOutputStream fos = new FileOutputStream("mnt/sdcard/File/" +files.get(i).getName());
                //read file

                while (fileSize.get(i) > 0 && (n = dis.read(buf, 0, (int)Math.min(buf.length, fileSize.get(i)))) != -1)
                {
                    fos.write(buf,0,n);
                    long x = fileSize.get(i);
                    x = x-n;
                    fileSize.set(i,x);
                }
                fos.close();
            }

            serversocket.close();
            System.out.println("Stopped listening, Server socket is now closed.");
        }
        catch (IOException e) {
            xceptionFlag = true;
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (!xceptionFlag){
            mServerBtn.setEnabled(true);
            Toast.makeText(context,"files Received Successfully!!",Toast.LENGTH_LONG).show();
        }
        else{
            mServerBtn.setEnabled(true);
            Toast.makeText(context,"Oops, Something went wrong.",Toast.LENGTH_LONG).show();
        }
    }
}
