package com.example.finalproject.ui.usersearch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.finalproject.Helper;
import com.example.finalproject.R;
import com.example.finalproject.WifiDirectBroadcastReceiver;
import com.example.finalproject.data.Database;
import com.example.finalproject.data.models.HistoryModelEntity;
import com.example.finalproject.data.models.MessageModelEntity;
import com.example.finalproject.ui.MainActivity;
import com.example.finalproject.ui.messages.MessageContract;
import com.example.finalproject.ui.messages.MessageFragment;
import com.example.finalproject.ui.models.HistoryModel;
import com.example.finalproject.ui.models.MessageModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserSearchPresenter implements UserSearchContract.Presenter {

    private UserSearchContract.View view;
    private Context context;
    private boolean isPaused;
    private boolean isConnected;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;

    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    List<WifiP2pDevice> peers = new ArrayList<>();

    HistoryModel clickedModel;

    WifiP2pDevice connectedDevice;

    boolean reconnecting;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    public UserSearchPresenter(UserSearchContract.View view, Context context) {
        this.view = view;
        this.context = context;
        this.isPaused = false;
        this.isConnected = false;
        this.clickedModel = null;
        this.connectedDevice = null;
        this.reconnecting = false;

        setupBroadcastReceiver();
        disconnect();
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void searchUsers() {
        view.showProgressBar();
        setupDiscover();
    }

    private void setupBroadcastReceiver() {
        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context.getApplicationContext(), context.getMainLooper(), null);

        receiver = new WifiDirectBroadcastReceiver(wifiP2pManager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
    }

    private WifiP2pManager.ActionListener discoverListener = new WifiP2pManager.ActionListener() {

        @Override
        public void onSuccess() {
//                Toast.makeText(context, "Discovering peers...", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int reason) {
            Toast.makeText(context, "Discovering peers failed. reason: " + reason, Toast.LENGTH_SHORT).show();
        }
    };

    private WifiP2pManager.ActionListener stopDiscoverListener = new WifiP2pManager.ActionListener() {

        @Override
        public void onSuccess() {
//                Toast.makeText(context, "Peers discovery stopped", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailure(int reason) {
            Toast.makeText(context, "Peers discovery stop failed. reason: " + reason, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void setupDiscover() {
        wifiP2pManager.discoverPeers(channel, discoverListener);
    }

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            view.hideProgressBar();

            List<HistoryModel> result = new ArrayList<>();

            if (!wifiP2pDeviceList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());

                for (WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()) {
                    result.add(new HistoryModel(device.deviceName, device));
                }
            }

            if (peers.size() == 0) {
                Toast.makeText(context.getApplicationContext(), "No devices found", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(context.getApplicationContext(), peers.size() + " Devices", Toast.LENGTH_SHORT).show();

            view.showData(result);
        }
    };

    @Override
    public void stopDiscovery() {
        peers = new ArrayList<>();
        view.showData(new ArrayList<HistoryModel>());
        wifiP2pManager.stopPeerDiscovery(channel, stopDiscoverListener);
    }

    private void cancelConnect() {
        wifiP2pManager.cancelConnect(channel, cancelListener);
    }

    @Override
    public void onResume() {
        isPaused = false;
        registerReceiver();
    }

    @Override
    public void onPause() {
        unregisterReceiver();
        isPaused = true;
        stopDiscovery();

        disconnect();
    }

    @Override
    public void onStop() {
        int x = 5;
    }


    @Override
    public boolean isPaused() {
        return this.isPaused;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver();
        isPaused = true;
        stopDiscovery();

        disconnect();


    }

    private void closeSocket() {
        if (serverClass == null || serverClass.socket == null) return;
        try {
            serverClass.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("main", "removeGroup onSuccess");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("main", "removeGroup onFailure -" + reason);
            }
        });

        cancelConnect();

        deleteGroups();

        closeSocket();
    }

    private void deleteGroups() {
        Method deletePersistentGroupMethod = null;
        try {
            deletePersistentGroupMethod = WifiP2pManager.class.getMethod("deletePersistentGroup", WifiP2pManager.Channel.class, int.class, WifiP2pManager.ActionListener.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        for (int netid = 0; netid < 32; netid++) {
            try {
                deletePersistentGroupMethod.invoke(wifiP2pManager, this.channel, netid, null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setConnected(boolean connected) {
        this.isConnected = connected;
    }

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public WifiP2pManager.PeerListListener getPeerListListener() {
        return peerListListener;
    }

    @Override
    public void registerReceiver() {
        receiver = new WifiDirectBroadcastReceiver(wifiP2pManager, channel, this);
        context.registerReceiver(receiver, intentFilter);
    }

    @Override
    public void unregisterReceiver() {
        context.unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public HistoryModel addUser(HistoryModel model) {
        model.setLastMessageDate(new Date());
        long id = Database.getInstance().dataDao().insertHistory(FromModelToEntity(model));
        model.setId(id);

        return model;
    }

    @Override
    public void setConnectedDevice(WifiP2pDevice device) {
        connectedDevice = device;
    }

    private WifiP2pManager.ActionListener connectListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(context.getApplicationContext(), "Connected to " + clickedModel.getDevice().deviceName, Toast.LENGTH_SHORT).show();
            view.changeStatus("Connected");
        }

        @Override
        public void onFailure(int reason) {
            String deviceName = "device";
            if (clickedModel != null)
                deviceName = clickedModel.getDevice().deviceName;
            Toast.makeText(context, "Could not connect " + deviceName + " reason: " + reason, Toast.LENGTH_SHORT).show();
        }
    };

    private WifiP2pManager.ActionListener cancelListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            Toast.makeText(context.getApplicationContext(), "Cancel connect successful", Toast.LENGTH_SHORT).show();
            view.changeStatus("Not Connected");
            if (reconnecting) {
                reconnecting = false;
                connectToDevice();
            }
        }

        @Override
        public void onFailure(int reason) {
            Toast.makeText(context.getApplicationContext(), "Cancel connect failed", Toast.LENGTH_SHORT).show();

            if (reconnecting) {
                reconnecting = false;
                connectToDevice();
            }
        }
    };

    @Override
    public void chatClicked(HistoryModel model) {
        clickedModel = model;

        this.reconnecting = true;
        // it will connect in success
        wifiP2pManager.cancelConnect(channel, cancelListener);
    }

    private void connectToDevice() {
        final WifiP2pDevice device = clickedModel.getDevice();
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        wifiP2pManager.connect(channel, config, connectListener);
    }

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                // Host
                view.changeStatus("Host");
                serverClass = new ServerClass();
                serverClass.start();
            } else if (wifiP2pInfo.groupFormed) {
                // Client
                view.changeStatus("Client");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    private void gotoChat(HistoryModel model) {
        model = addUser(model);

        Bundle args = new Bundle();
        args.putSerializable("NewHistoryModel", model);

        NavController navController = Navigation.findNavController((MainActivity) context, R.id.main_fragment);
        navController.navigate(R.id.action_findUserFragment_to_messageFragment, args);
    }

    private int counter = 1;

    @Override
    public void onMessageSend() {
        if (sendReceive == null) return;
        Toast.makeText(context, "Sending message " + counter, Toast.LENGTH_SHORT).show();

        String msg = "test message" + (counter++);
        sendReceive.write(msg.getBytes());
    }

    @Override
    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() {
        return this.connectionInfoListener;
    }

    private HistoryModelEntity FromModelToEntity(HistoryModel model) {
        return new HistoryModelEntity(model.getName(), Helper.fromDate(model.getLastMessageDate()), model.getMessageCount());
    }

    static final int MESSAGE_READ = 1;


    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(7878);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();

                HistoryModel partner = clickedModel;
                if (partner == null) partner = new HistoryModel(connectedDevice.deviceName, connectedDevice);

                gotoChat(partner);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) message.obj;
                    String tempMsg = new String(readBuff, 0, message.arg1);
                    // msg in tempmsg
                    view.setMessage(tempMsg);
                    break;
            }
            return true;
        }
    });

    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                new SendMessageThread().execute(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private class SendMessageThread extends AsyncTask<byte[], Void, Void> {

            @Override
            protected Void doInBackground(byte[]... bytesArr) {
                try {
                    outputStream.write(bytesArr[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 7878), 500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();

                if (clickedModel == null && connectedDevice == null) {
                    Toast.makeText(context, "Device not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                HistoryModel partner = clickedModel;
                if (partner == null) partner = new HistoryModel(connectedDevice.deviceName, connectedDevice);

                gotoChat(partner);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
