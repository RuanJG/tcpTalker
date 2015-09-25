package com.example.joe.tcptalkapplication;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;


public class MainActivity extends Activity {

    private  EditText console;
    private EditText inputText;
    private Button button;
    private Socket socket;
    private Thread tcpReciveThread;
    private Thread tcpSendThread;
    private boolean threadStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        console = (EditText) this.findViewById(R.id.console);
        inputText = (EditText) this.findViewById(R.id.inputText);
        button = (Button) this.findViewById(R.id.button);
        tcpSendThread = new Thread(tcpSendRunner);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(tcpSendRunner).start();
            }
        });
    }
    @Override
    protected void onDestroy() {
        if( socket != null && socket.isConnected()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void connectServerWithTCPSocket() {
        try {
            if( socket != null && socket.isConnected()) {
                Log.e("Ruan: ","has connect !!");
                return;
            }
            Log.e("Ruan: ", "try to connect !!");
            socket = new Socket("192.168.2.1", 6666);
            socket.setSoTimeout(10000);
            if( socket != null && socket.isConnected() ) {
                Log.e("Ruan: ","+connect !!");
                startTcpThread();
            }else
                Log.e("Ruan: ","+connect false !!");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.e("Ruan: ", "+connect false !!");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Ruan: ", "+connect false !!");
        }

    }
    boolean startWait = false;
    private  void sendMessage(String message)
    {
        String msg = message  + "\r\n";
        //console.append(msg);
        if( socket==null || !socket.isConnected()){
            connectServerWithTCPSocket();
        }
        if( socket == null || !socket.isConnected())
            return;

        try
        {
            Log.e("ruan", "start send " + message);
            //PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true);
            //out.print(msg);
            OutputStream outputStream = socket.getOutputStream();
            //byte buffer[] = new byte[4 * 1024];
            outputStream.write(message.getBytes());
            outputStream.flush();
            startWait = true;
        }
        catch (Exception e)
        {
            // TODO: handle exception
            Log.e("Ruan:", e.toString());
        }
    }

    private void notifyConsole(String msg)
    {
        if(msg != null){
            Message mesg = new Message();
            Bundle bun = new Bundle();
            bun.putString("data",msg);
            mesg.setData(bun);
            mHandler.sendMessage(mesg);
            Log.e("ruan","I get message "+msg);
        }else{
            Log.e("ruan","no message");
        }
    }
    DataInputStream dataInputStream = null;
    private void getMessage()
    {
        String msg;
        boolean isGpsData = false;

        if( socket!=null && !socket.isConnected()) {
            msg = null;
            return;
        }
        try {

            if( dataInputStream == null)
                dataInputStream = new DataInputStream(socket.getInputStream());
            byte[] tempByte = new byte[1024];
            char[] tempChar = new char[1024];
            msg="";
            while(true){
                int len = dataInputStream.read(tempByte);
                Log.e("ruan", "get len=" + len);
                for( int i =0; i< len; i++) {
                    Log.e("ruan", "," + tempByte[i]);
                    if( tempByte[i] < 0 )
                        tempChar[i]=(char)(tempByte[i]+256);
                    else
                        tempChar[i]=(char)tempByte[i];

                    if( tempChar[i] == '\r' || tempChar[i] == '\n') {
                        if( msg!=null && msg.length()>0 ) {
                            if (msg.length() > 70 && isGpsData){
                                notifyConsole(msg + '\n');
                                isGpsData = false;
                            }else
                                notifyConsole(msg + '\n');
                            Log.e("ruan:","msg="+msg);
                            Log.e("ruan:","msg len="+msg.length());
                            if( msg.equals("OK")) {
                                break;
                            }
                        }
                        msg = "";
                        continue;
                    }
                    msg = msg+tempChar[i];
                    if( msg.equals("+CGPSINF: ")){
                        //next string is gps data
                        msg = "";
                        isGpsData = true;
                    }
                }
                if( msg.equals("OK")) {
                    //notifyConsole(msg);
                    break;
                }
            }


        }catch (Exception e) {
                // TODO: handle exception
                Log.e("Ruan:", e.toString());
            msg = null;
        }
        return;


    }
    private  void startTcpThread() {
        if (tcpReciveThread == null){
            threadStop = false;
            tcpReciveThread = new Thread(tcpReciveRunner);
            tcpReciveThread.start();
        }
        //tcpThread.start();
    }

    Runnable tcpReciveRunner = new Runnable() {
        @Override
        public void run() {
            while (!threadStop){
                getMessage();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.e("ruan","tcpreciveRunner exit");
        }
    };
    Runnable tcpSendRunner = new Runnable() {
        @Override
        public void run() {
            String msg = inputText.getText().toString();
            sendMessage(msg+"\r\n");
            Log.e("ruan", "tcpsendRunner exit");
        }
    };

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            if( console == null) {
                console = (EditText) findViewById(R.id.console);
            }

            console.append(msg.getData().getString("data"));

            super.handleMessage(msg);
        }
    };


}
