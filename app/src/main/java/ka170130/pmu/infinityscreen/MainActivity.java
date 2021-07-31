package ka170130.pmu.infinityscreen;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import ka170130.pmu.infinityscreen.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    public final static String LOG_TAG = "default-log-tag";

    private ActivityMainBinding binding;
    private WifiDeviceViewModel wifiDeviceViewModel;

    // Concurrent
    private ExecutorService executorService;
    private BlockingQueue<String> messageQueue;

    // WiFi P2P
    private WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private WifiP2pInfo info;

    private MediaPlayer mediaPlayer;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    grantedMap -> {
                        Iterator<String> iterator = grantedMap.keySet().iterator();
                        boolean allGranted = true;

                        while (allGranted && iterator.hasNext()) {
                            String key = iterator.next();
                            allGranted = grantedMap.get(key);
                        }

                        if (allGranted) {
                            onPermissionGranted();
                        }
                    }
            );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
//                try {
//                    mediaPlayer.setDataSource(this, result);
//                    mediaPlayer.setOnPreparedListener(MediaPlayer::start);
//                    mediaPlayer.prepareAsync();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                executorService.submit(() -> {
                    Socket socket = new Socket();
                    OutputStream outputStream = null;
                    InputStream inputStream = null;

                    try {
                        File file = new File(getApplicationContext().getExternalFilesDir("ffmpeg"), "infinity-screen-" + System.currentTimeMillis() + ".mp4");

                        File dirs = new File(file.getParent());
                        if (!dirs.exists()) {
                            dirs.mkdirs();
                        }
                        // ffmpeg creates the file so we don't have to
                        // file.createNewFile();

                        ContentResolver contentResolver = getApplicationContext().getContentResolver();
                        String srcPath = FilePathFromUri.getUriRealPath(this, contentResolver, result);

                        String command = "-ss 00:00:05.0 -i " + srcPath + " -c copy -to 00:00:08.0 " + file.getAbsolutePath();
                        Log.d(LOG_TAG, "FFmpeg command: " + command);
                        int rc = FFmpeg.execute(command);

                        if (rc != Config.RETURN_CODE_SUCCESS) {
                            Log.d(LOG_TAG, "FFmpeg command failed with rc: " + rc);
                            Config.printLastCommandOutput(Log.INFO);
                            throw new Exception();
                        }

                        // socket stuff
                        socket.bind(null);
                        socket.connect((new InetSocketAddress(info.groupOwnerAddress, 8888)), 500);

                        outputStream = socket.getOutputStream();

                        try {
                            inputStream = contentResolver.openInputStream(Uri.fromFile(file));
                        } catch (FileNotFoundException exception) {
                            exception.printStackTrace();
                        }

                        byte buf[] = new byte[1024];
                        int len;

                        try {
                            while ((len = inputStream.read(buf)) != -1) {
                                outputStream.write(buf, 0, len);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        // catch logic
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                // catch logic
                                e.printStackTrace();
                            }
                        }

                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                // catch logic
                                e.printStackTrace();
                            }
                        }

                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    // catch logic
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
            }
    );

    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener = info -> {
        Log.d(LOG_TAG, "Connection info is available!");
        this.info = info;

        if (info.isGroupOwner) {
            // show receiver views
            binding.messageEditText.setVisibility(View.GONE);
            binding.sendButton.setVisibility(View.GONE);
            binding.messageContent.setVisibility(View.VISIBLE);
            binding.videoButton.setVisibility(View.GONE);
            binding.textureView.setVisibility(View.VISIBLE);
        } else {
            // show sender views
            binding.messageEditText.setVisibility(View.GONE);
            binding.sendButton.setVisibility(View.GONE);
            binding.messageContent.setVisibility(View.GONE);
            binding.videoButton.setVisibility(View.VISIBLE);
            binding.textureView.setVisibility(View.GONE);
        }
    };

    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() {
        return connectionInfoListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        wifiDeviceViewModel = new ViewModelProvider(this).get(WifiDeviceViewModel.class);

        executorService = Executors.newFixedThreadPool(4);
        messageQueue = new LinkedBlockingDeque<>();

        mediaPlayer = new MediaPlayer();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiDirectBroadcastReceiver(wifiManager, manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



        binding.discoverButton.setOnClickListener(view -> tryToDiscoverPeers());

        // Discovery List Recycler View
        WifiDirectAdapter discoveryAdapter = new WifiDirectAdapter(device -> {
            connectToPeer(device);
        });
        wifiDeviceViewModel.getAvailableList().observe(this, list -> {
            discoveryAdapter.setDevices(new ArrayList<>(list));
        });

        binding.discoveryRecyclerView.setHasFixedSize(false);
        binding.discoveryRecyclerView.setAdapter(discoveryAdapter);
        binding.discoveryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Selected List Recycler View
        WifiDirectAdapter selectedAdapter = new WifiDirectAdapter(device -> {
            wifiDeviceViewModel.unselectDevice(device);
        });
        wifiDeviceViewModel.getSelectedList().observe(this, list -> {
            selectedAdapter.setDevices(new ArrayList<>(list));
        });

        binding.selectedRecyclerView.setHasFixedSize(false);
        binding.selectedRecyclerView.setAdapter(selectedAdapter);
        binding.selectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Send message
        binding.sendButton.setOnClickListener(view -> {
            String message = "";
            Editable text = binding.messageEditText.getText();
            if (text != null) {
                message = text.toString();
            }

            // messageQueue.put(message);

            String finalMessage = message;
            executorService.submit(() -> {
                Socket socket = new Socket();
                OutputStream outputStream = null;

                try {
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(info.groupOwnerAddress, 8888)), 500);

                    outputStream = socket.getOutputStream();

                    outputStream.write(finalMessage.getBytes());
                } catch (Exception e) {
                    // catch logic
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            // catch logic
                            e.printStackTrace();
                        }
                    }

                    if (socket != null) {
                        if (socket.isConnected()) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                // catch logic
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        });

        // Video button
        binding.videoButton.setOnClickListener(view -> {
            galleryLauncher.launch("video/*");
        });

        // Texture View
        binding.textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Surface s = new Surface(surface);
                mediaPlayer.setSurface(s);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                // do nothing
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                // do nothing
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // do nothing
            }
        });

        // Wait for message
        executorService.submit(() -> {
            ServerSocket serverSocket = null;

            try {
                serverSocket = new ServerSocket(8888);

                while (true) {
                    Socket client = serverSocket.accept();

                    executorService.submit(() -> {
                        try {
                            File file = new File(getApplicationContext().getExternalFilesDir("received"), "infinity-screen-" + System.currentTimeMillis() + ".mp4");

                            File dirs = new File(file.getParent());
                            if (!dirs.exists()) {
                                dirs.mkdirs();
                            }
                            file.createNewFile();

                            InputStream inputStream = client.getInputStream();
                            OutputStream outputStream = new FileOutputStream(file);

                            byte buf[] = new byte[1024];
                            int len;

                            try {
                                while ((len = inputStream.read(buf)) != -1) {
                                    outputStream.write(buf, 0, len);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            mediaPlayer.setDataSource(this, Uri.fromFile(file));
                            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
                            mediaPlayer.prepareAsync();

    //                        while (true) {
    //                            inputStream.read(buf);
    //
    //                            String message = new String(buf, StandardCharsets.UTF_8);
    //                            binding.messageContent.setText(message);
    //                        }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                // catch logic
                e.printStackTrace();
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(receiver);
    }

    private void tryToDiscoverPeers() {
        if (
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            String[] permissions = {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            requestPermissionsLauncher.launch(permissions);
        } else {
            onPermissionGranted();
        }
    }

    private void onPermissionGranted() {
        // Permission should already be granted, this code removes annoying red code color
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Discover Peers: SUCCESS");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LOG_TAG, "Discover Peers: FAILURE");
            }
        });
    }

    private void connectToPeer(WifiP2pDevice peer) {
        // Permission should already be granted, this code removes annoying red code color
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.deviceAddress;
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "Connected to " + peer.deviceName + " successfully!");
                wifiDeviceViewModel.selectDevice(peer);

                /*
                executorService.submit(() -> {
                    Socket socket = new Socket();
                    OutputStream outputStream = null;

                    try {
                        socket.bind(null);
                        socket.connect((new InetSocketAddress(peer.deviceAddress, 8888)), 500);

                        outputStream = socket.getOutputStream();

                        while (true) {
                            String message = messageQueue.take();
                            outputStream.write(message.getBytes());
                        }
                    } catch (Exception e) {
                        // catch logic
                        e.printStackTrace();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                // catch logic
                                e.printStackTrace();
                            }
                        }

                        if (socket != null) {
                            if (socket.isConnected()) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    // catch logic
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
                */
            }

            @Override
            public void onFailure(int reason) {
                Log.d(LOG_TAG, "Connection to " + peer.deviceName + " failed!");
            }
        });
    }
}