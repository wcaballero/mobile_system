
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;


public class  MessageActivity extends AppCompatActivity {
    private static final String TAG = "MessageActivity";

    // Deals with file creation and tracking
    private FileObserver fileObserver;
    private File file;
    private FileOutputStream fileOutputStream;
    private String filename = "myFile.txt";
    private String folderName = "BluetoothApp";
    private long fileTimeStamp;

    Button btnSend;
    EditText etSend;

    TextView incomingMessages;
    StringBuilder messages;

    BluetoothConnectionService mBluetoothConnection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_view);
        file = new File(getBaseContext().getExternalFilesDir(folderName), filename);
        saveToFile(""); // when message activity is created we also create a empty file
        /* FileObserver will create a new thread */
        fileObserver = new FileObserver(file.getPath()) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                Log.d(TAG, "FileObserver has been triggered");
                long currTimeStamp = file.lastModified();
                if (currTimeStamp != fileTimeStamp) {
                    Date newDate = new Date(currTimeStamp);
                    fileTimeStamp = currTimeStamp;

                    Log.d(TAG, "File Observer has modified file, newest time stamp is " + newDate);
                    fileToUIScreen();

                    FileInputStream inStream = null;
                    StringBuilder inMessage = new StringBuilder();
                    int inChar;
                    try {
                        inStream = new FileInputStream(file);
                        while ((inChar = inStream.read()) != -1) {
                            inMessage.append((char)inChar);
                        }
                        mBluetoothConnection.write(inMessage.toString().getBytes());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            assert inStream != null;
                            inStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        };
        fileObserver.startWatching();

        mBluetoothConnection = BluetoothConnectionService.getInstance();

        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);

        incomingMessages = (TextView) findViewById(R.id.incomingMessage);
        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                Log.d(TAG, "The btn send: " + etSend.getText().toString() + "\n\n");

                saveToFile(etSend.getText().toString());
                mBluetoothConnection.write(bytes);
//                messages.append("Sent: " + etSend.getText().toString() + "\n");
                fileToUIScreen();
//                incomingMessages.setText(messages);
                etSend.setText("");

                Context context = getApplicationContext();
                CharSequence text = "File Sent";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        });
    }

    // Display received messages
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");

            saveToFile(text);
            fileToUIScreen();
            incomingMessages.setText(messages);

        }
    };

    /**
     * Saves a string to a file or appends if the file exist.
     * @param data - The data that needs to be appended to a file
     * return true if success false otherwise
     */
    private void saveToFile(String data, boolean append) {
        data = data == "" ? "" : data + "\n";
        try {
            File directoryToStore;
            directoryToStore = getBaseContext().getExternalFilesDir(folderName);
            if (!directoryToStore.exists()) {
                if (directoryToStore.mkdir()) ; //directory is created;
            }

            fileOutputStream = new FileOutputStream(file, append);
            fileOutputStream.write(data.getBytes());
            fileTimeStamp = file.lastModified(); // returns milliseconds
            Log.d(TAG, "wrote to file: " + getFilesDir());
            Date lastModDate = new Date(fileTimeStamp);
            Log.d(TAG, "The file was last modified: " + lastModDate);
        } catch (Exception e) {
            Log.d(TAG, "could not save to file");
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                Log.d(TAG, "could not close file successfully");
                e.printStackTrace();
            }
        }
    }

    private void saveToFile(String data) {
        saveToFile(data, false);
    }

    /**
     * Reads file and overwrites UI screen with content
     * If usi
     * @throws IOException
     */
    public void fileToUIScreen() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                // Stuff that updates the UI
                incomingMessages.setText("");
                FileInputStream fileInputStream = null;
                messages = new StringBuilder();
                int ch;
                try {
                    fileInputStream = new FileInputStream(file);
                    while ((ch = fileInputStream.read()) != -1) {
                        messages.append((char)ch);
                    }
                    incomingMessages.setText(messages);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onReceive: STATE OFF");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
            Log.d(TAG, "mReceiver unregistered");

        } catch (Exception e) {
            Log.d(TAG, "mReceiver not registered");
        }
    }
}
