

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class Sender extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "Sender";
    private static final int PORT = 5004;
    private Socket socket;

    private boolean xceptionFlag = false;

    private Context context;
    private String destinationAddress;
    private String[] filePaths;



    // constructor to get files and where the files will be going
    Sender(Context context, String[] files, String destinationAddress){
        this.context = context;
        this.filePaths = files;    // list of files the user selected
        this.destinationAddress = destinationAddress;  // the distination at which the files are going to
    }

    @Override
    protected Void doInBackground(Void... voids) {

        System.out.println("array list");
        ArrayList<File> files = new ArrayList<>();
        System.out.println("about to create.");

        // get all of the file path and create them
        for(int i = 0; i < filePaths.length; i++){
           files.add(new File(filePaths[i]));
           Log.d(TAG, i + "File added: " + filePaths[i]);
        }

        try{
            // create a socket to send data and connect it to the IP address and port
            socket = new Socket(InetAddress.getByName(destinationAddress), PORT);
            Log.d(TAG, "Socket is opened and connected to " + destinationAddress + " and the port number is " + PORT);

            System.out.println("Connecting...");
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            System.out.println(files.size());

            //write the number of files to the server
            dos.writeInt(files.size());
            dos.flush();

            //write file size
            for(int i = 0;i< files.size();i++){
                int file_size = Integer.parseInt(String.valueOf(files.get(i).length()));
                dos.writeLong(file_size);
                dos.flush();
            }

            //write file names
            for(int i = 0 ; i < files.size();i++){
                dos.writeUTF(files.get(i).getName());
                dos.flush();
            }

            //buffer for file writing, to declare inside or outside loop?
            int n = 0;
            byte[]buf = new byte[4092];
            //outer loop, executes one for each file
            for(int i =0; i < files.size(); i++){

                System.out.println(files.get(i).getName());
                //create new fileinputstream for each file
                FileInputStream fis = new FileInputStream(files.get(i));

                //write file to dos
                while((n =fis.read(buf)) != -1){
                    dos.write(buf,0,n);
                    dos.flush();

                }
                //should i close the dataoutputstream here and make a new one each time?
            }
            dos.close();

        }catch (IOException e){
            xceptionFlag = true;
            e.printStackTrace();
        }
        finally{
            if(socket != null && socket.isConnected()){
                System.out.println("Socket connection closed");
                try {
                    socket.close();
                } catch (IOException e) {
                    xceptionFlag = true;
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    protected void onPostExecute(Void aVoid){
        super.onPostExecute(aVoid);
        if(xceptionFlag){
            Toast.makeText(context, "Something fishy is going on.", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(context, "Sending file to " + destinationAddress, Toast.LENGTH_LONG).show();
        }
    }
}
