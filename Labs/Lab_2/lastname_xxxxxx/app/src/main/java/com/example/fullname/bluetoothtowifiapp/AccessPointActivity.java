

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import android.os.FileObserver;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;

// Third party packages
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import static com.github.angads25.filepicker.view.FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT;

/**
 * Handles the buttons clicks for sending and receiving a file.
 * Send will bring up a file picker dialogue to select and send the file to the other device.
 * Receive will bring up a progress dialogue box and wait for a file.
 */
public class AccessPointActivity extends Activity {
    private static final String TAG = "AccessPointActivity";
    Button sendIpBtn;
    Button clientBtn;
    Button serverBtn;
    ListView listView;

    public static final int EXTERNAL_READ_PERMISSION_GRANT=112;

    TextView incomingMessages;
    StringBuilder ipAddresses;

    BluetoothConnectionService mBluetoothConnection;
    String deviceIP;
    String incomingIP;

    InetAddress serverInetAddress = null;

    private FileObserver fileObserver;
    String filePath = "/mnt/sdcard/File/helloWorld.txt";
    File mFile = new File(filePath);
    FilePickerDialog dialog;
    static long fileTimeStamp;

    private Socket socket;



    /**
     * Called once when launching access point activity.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.access_point_view);

        fileObserver = new FileObserver("mnt/sdcard/File/helloWorld.txt") {
            @Override
            public void onEvent(int event, @Nullable String path) {
                Log.d(TAG, "Fileobserver has been triggered");
                long currTimeStamp = mFile.lastModified();
                if (incomingIP != null && currTimeStamp != fileTimeStamp) {
                    Date newDate = new Date(currTimeStamp);
                    fileTimeStamp = currTimeStamp;
                    Log.d(TAG, "File Observer has modified file, newest time stamp is " + newDate);
                    try {
                        socket = new Socket(InetAddress.getByName(incomingIP), 5004);
                        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                        dos.writeInt(1);
                        dos.flush();
                        dos.writeLong(mFile.length());
                        dos.flush();
                        dos.writeUTF(mFile.getName());
                        dos.flush();

                        int n = 0;
                        byte[]buf = new byte[4092];
                        System.out.println(mFile.getName());
                        FileInputStream fis = new FileInputStream(mFile);

                        //write file to dos
                        while((n =fis.read(buf)) != -1){
                            dos.write(buf,0,n);
                            dos.flush();

                        }
                        dos.close();

                        socket.close();
                        Log.d(TAG, "Sent file from fileobserver");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        fileObserver.startWatching();

        listView = (ListView) findViewById(R.id.fileList);
//       status = (TextView) findViewById(R.id.statusTV);
        sendIpBtn = findViewById(R.id.sendIpBtn);

        mBluetoothConnection = BluetoothConnectionService.getInstance();

        //Device IP address
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        deviceIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d(TAG, "IP: " + deviceIP);

        try {
            serverInetAddress = InetAddress.getByName(deviceIP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        incomingMessages = (TextView) findViewById(R.id.incomingMessagesView);
        ipAddresses = new StringBuilder();

        // Connecting device IP
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        // Implement runtime permissions
        if (!checkPermissionForReadExternalStorage()) {
            try {
                requestPermissionForReadExternalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!checkPermissionForWriteExternalStorage()) {
            try {
                requestPermissionForWriteExternalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        clientBtn = (Button) findViewById(R.id.clientBtn);


        serverBtn = (Button) findViewById(R.id.serverBtn);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Receive Pressed");
                serverBtn.setEnabled(false);
//                status.setText("Received Files");
                Receiver receiver = new Receiver(AccessPointActivity.this, AccessPointActivity.this, serverBtn);
                receiver.execute();
            }
        });


        sendIpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = deviceIP.getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
                sendIpBtn.setEnabled(false);
            }
        });
    }


    public void sendFiles(View view) {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;

//        status.setText("Shared Files");

        dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select files to share");

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (null == files || files.length == 0) {
                    Toast.makeText(AccessPointActivity.this, "Select at least one file to start Share Mode", Toast.LENGTH_SHORT).show();
                    return;
                }

//                ArrayAdapter<String> filesAdapter =
//                        new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, Arrays.asList(files));
//                listView.setAdapter(filesAdapter);

                Sender sender = new Sender(getApplicationContext(), files, incomingIP);
                sender.execute();

            }
        });
        dialog.show();
    }


    // Display received messages
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            incomingIP = intent.getStringExtra("theMessage");

            ipAddresses.append("Connected to " + incomingIP + "\n");
            incomingMessages.setText(ipAddresses);
            fileTimeStamp = mFile.lastModified();

        }
    };

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

    // Check storage permissions
    public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    EXTERNAL_READ_PERMISSION_GRANT);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public void requestPermissionForWriteExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_READ_PERMISSION_GRANT);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}