package com.ledv0;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Esta clase realiza todo el trabajo para configurar y administrar la conexion Bluetooth con otro dispositivo.
 * Hay un hilo que esta a la escucha de conexiones entrantes, un hilo para conexiones con un dispositivo y
 * un hilo para realizar las transmisiones una vez que se este conectado.
 */
public class ConexionBT {
	
	//<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Definiciones >>>>>>>>>>>>>>>>>>>>>>>>>>>
	
    // Debug Para monitorizar los eventos
    private static final String TAG = "Servicio_Bluetooth";
    private static final boolean D = true;
    
    // Nombre para el registro SDP cuando el socket sea creado
    private static final String NAME = "BluetoothDEB";

    // UUID Identificador unico de URI para esta aplicacion
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // UUID para chat con otro Android      ("fa87c0d0-afac-11de-8a39-0800200c9a66");         
    // UUID para modulos BT RN42            ("00001101-0000-1000-8000-00805F9B34FB");

    // Campos de coexion
    private final BluetoothAdapter AdaptadorBT;
    private final Handler mHandler;
    private AcceptThread HebraDeAceptacion;
    private ConnectThread HiloDeConexion;
    private ConnectedThread HiloConetado;
    private int EstadoActual;

    // Constantes que indican el estado de conexion
    public static final int STATE_NONE = 0;       // No se esta haciendo nada
    public static final int STATE_LISTEN = 1;     // Escuchando por conexiones entrantes
    public static final int STATE_CONNECTING = 2; // Iniciando conexion saliente
    public static final int STATE_CONNECTED = 3;  // Conectado con un dispositivo 
        
	//<<<<<<<<<<<<<<<<<<<<<<<<<<<<< METODOS >>>>>>>>>>>>>>>>>>>>>>>>>>>    
    //----<<<<---->>>>----METODOS  NO  ALTERADOS----<<<<---->>>>----

    /**
     * Constructor. Prepara una nueva sesion para la conexion Bluetooth Smartphone-Dispositivo
     * @param context  El identificador UI de la actividad de context
     * @param handler  Un Handler para enviar mensajes de regreso a la actividad marcada por el UI
     * 
     * */
    public ConexionBT(Context context, Handler handler) {
    	AdaptadorBT = BluetoothAdapter.getDefaultAdapter();
    	EstadoActual = STATE_NONE;
        mHandler = handler;
    }

 
    /**
     * Actualizamos estado de la conexion BT a la actividad 
     * @param estado  Un entero definido para cada estado
     */
    private synchronized void setState(int estado) {
        EstadoActual = estado;
    // Le enviamos al Handler el nuevo estado actual para que se actualize en la Actividad
        mHandler.obtainMessage(MainActivity.Mensaje_Estado_Cambiado, estado, -1).sendToTarget();
    }

       
    /**
     * Regresa el estado de la conexion */
    public synchronized int getState() {
        return EstadoActual;
    }

   
    /**
     * Inicia el servicio bluetooth. Especificamente inicia la HebradeAceptacion para iniciar el 
     * modo de "listening". LLamado por la Actividad onResume() */
    public synchronized void start() {
        if (D) Log.e(TAG, "start");

        //Cancela cualquier hilo que quiera hacer una conexion
        if (HiloDeConexion != null) {HiloDeConexion.cancel(); HiloDeConexion = null;}

        //Cancela cualquier hebra que este corriendo una conexion
        if (HiloConetado != null) {HiloConetado.cancel(); HiloConetado = null;}

        // Inicia la hebra que escuchara listen en el BluetoothServerSocket
        if (HebraDeAceptacion == null) {
        	HebraDeAceptacion = new AcceptThread();
        	HebraDeAceptacion.start();
        }
        setState(STATE_LISTEN);
    }

   
    /**
     * Inicia el HiloConectado para iniciar la conexion con un dispositivo remoto
     * @param device  -->El dispositivo BT a conectar
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.e(TAG, "Conectado con: " + device);
        
        //Cancela cualquier hilo que intente realizar una conexion 
        if (EstadoActual == STATE_CONNECTING) {
            if (HiloDeConexion != null) {HiloDeConexion.cancel(); HiloDeConexion = null;}  }

        //Cancela cualquier hilo que se encuentre corriendo una conexion
        if (HiloConetado != null) {HiloConetado.cancel(); HiloConetado = null;}

        //Inicia el hilo para conectar con un dispositivo
        HiloDeConexion = new ConnectThread(device);
        HiloDeConexion.start();
        setState(STATE_CONNECTING);
    }
    
    
    /**
     * Inicia la hebra conectada para iniciar la administracion de la conexión BT
     * @param socket  El socket Bt donde se realizara la conexion
     * @param device  El dispositivo BT con que se conectara
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.e(TAG, "connected");

        // Cancela el hilo  que completo la conexion
        if (HiloDeConexion != null) {HiloDeConexion.cancel(); HiloDeConexion = null;}

        //Cancela el hilo que actualmente esta corriendo la conexion 
        if (HiloConetado != null) {HiloConetado.cancel(); HiloConetado = null;}

        // Cancela la Hebradeaceptacion debido a que solo queremos conectar con un dispositivo**********
        if (HebraDeAceptacion != null) {HebraDeAceptacion.cancel(); HebraDeAceptacion = null;}

        //Inicia el hilo para administrar la conexion y realizar transmisiones
        HiloConetado = new ConnectedThread(socket);
        HiloConetado.start();

        //Envia el nombre del dispositivo conectado de vuelta 
        Message msg = mHandler.obtainMessage(MainActivity.Mensaje_Nombre_Dispositivo);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

 
    /**
     * Para todos los hilos y pone el estado de STATE_NONE donde no esta haciendo nada
     */
    public synchronized void stop() {
        if (D) Log.e(TAG, "stop");
        if (HiloDeConexion != null) {HiloDeConexion.cancel(); HiloDeConexion = null;}
        if (HiloConetado != null) {HiloConetado.cancel(); HiloConetado = null;}
        if (HebraDeAceptacion != null) {HebraDeAceptacion.cancel(); HebraDeAceptacion = null;}
        setState(STATE_NONE);
    }


    /**
     * Escribe en el HiloConectado de manera Asincrona
     * @param out Los bytes a escribir
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        ConnectedThread r; //Creacion de objeto temporal
        // Syncronizar la copia del HiloConectado
        synchronized (this)    {
            if (EstadoActual != STATE_CONNECTED) return;
            r = HiloConetado;  }
        // Realizar la escritura Asincrona
        r.write(out);
    }

   
    /**
     * Indica que el intento de conexion fallo y notifica a la actividad WidgetProvider/UpdateService
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);
        //Envia un mensaje de falla de vuelta a la actividad
        Message msg = mHandler.obtainMessage(MainActivity.Mensaje_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Error de conexi�n");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }


    /**
     * Indica que la conexion se perdio y notifica a la UI activity(WidgetProvider/UpdateSErvice)
     */
    private void connectionLost() {
        setState(STATE_LISTEN);
        //Envia un mensaje de falla de vuelta a la actividad 
        Message msg = mHandler.obtainMessage(MainActivity.Mensaje_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Se perdio conexion");
        msg.setData(bundle);                       
        mHandler.sendMessage(msg);             
        msg = mHandler.obtainMessage(MainActivity.MESSAGE_Desconectado);                   
        mHandler.sendMessage(msg);
  
        
    }

    
    /**
     * Este hilo corre mientras se este ESCUCHANDO por conexiones entrantes. Este se 
     * comporta como el lado-Servidor cliente. Corre mientras la conexion es aceptada(o cancelada)
     */
    private class AcceptThread extends Thread {
        // El soket de servidor Local
        private final BluetoothServerSocket mmServerSocket;
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public AcceptThread() { 
            BluetoothServerSocket tmp = null;
            //Creamos un nuevo listening server socket
            try {
                tmp = AdaptadorBT.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() fallo", e);
            }
            mmServerSocket = tmp;
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public void run() {
            if (D) Log.e(TAG, "Comenzar HiloDeAceptacion " + this);
            setName("HiloAceptado");
            BluetoothSocket socket = null;
            //Escucha al server socket si no estamos conectados
            while (EstadoActual != STATE_CONNECTED) {
                try {
                    //Esto es un  bloque donde solo obtendremos una conexion o una excepcion
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                //Si la conexion fue aceptada...
                if (socket != null) {
                    synchronized (ConexionBT.this) {
                        switch (EstadoActual) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Iniciamos HebraConectada
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // O no esta lista o ya esta conectado. Termina el nuevo socket
                        	try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "No se pudo cerrar el socket no deseado", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.e(TAG, "Fin de HIlodeAceptacion");
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public void cancel() {
            if (D) Log.e(TAG, "Cancela " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() del servidor FAllo", e);
            }
        }
 //<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>        
    }

  
    /**
     * Esta Hebra correra mientras se intente realizar una conexion de salida con un dispositivo.
     * Este correra a través de la conexion ya sea establecida o fallada
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Obtiene un BluetoothSocket para la conexion con el Dispositivo obtenido
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() Fallo", e);
            }
            mmSocket = tmp;
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public void run() {
            Log.e(TAG, "Comenzando HebraConectada");
            setName("HiloConectado");
            //Siempre cancela la busqueda debido a que esta hara lenta la conexion
            AdaptadorBT.cancelDiscovery();
            // Realiza la conexion con el socketBluetooth
            try {
                // Aqui solo recibiremos o una conexion establecida o una excepcion
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Cierra el socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Imposible cerrar el socket durante la falla de conexion", e2);
                }
                // Inicia el servicio a traves de reiniciar el modo de listening
                ConexionBT.this.start();
                return;
            }
            // Resetea el HiloConectado pues ya lo hemos usado
            synchronized (ConexionBT.this) {
            	HiloDeConexion = null;
            }
            // Inicia el hiloconectado
            connected(mmSocket, mmDevice);
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    }//fin de conectThread

    

    /**
     * Este hilo corre durante la conexion con un dispositivo remoto.
     * Este maneja todas las transmisiones de entrada y salida.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket BTSocket;
        private final InputStream INPUT_Stream;
        private final OutputStream OUTPUT_Stream;
        
        
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "Creacion de HiloConectado");
            BTSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Obtencion del BluetoothSocket de entrada y saldida
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Sockets temporales No creados", e);
            }
            INPUT_Stream = tmpIn;
            OUTPUT_Stream = tmpOut;
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        /**
         * Escribe al Stream de salida conectado
         * @param buffer  Los bytes a escribir
         */
        public void write(byte[] buffer) {
            try {
            	OUTPUT_Stream.write(buffer); //Compartir el mensaje enviado con la UI activity
                mHandler.obtainMessage(MainActivity.Mensaje_Escrito, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {Log.e(TAG, "Exception during write", e);}
        }//FIN DE WRITE
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
        public void cancel() {
            try {
            	BTSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() del socket conectado Fallo", e);
            }
        }
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>        
        
        public void run() {
            Log.e(TAG, "Comenzar Hebraconectada");
            byte[] buffer = new byte[1024];
            int bytes;
           
            while (true) { //Mantiene escuchando el InputStream mientras este conectado
  try {           	               	
            //Lee desde el InputStream
  bytes = INPUT_Stream.read(buffer);   
  // byte[] readBufX = (byte[]) buffer;  //Construye un String desde los bytes validos en el buffer
 // String readMessageX = new String(readBufX, 0, bytes);//Se envia el readNessagexxx en lugar del buffer pues ya se PARSEO               
      mHandler.obtainMessage(MainActivity.Mensaje_Leido, bytes, -1, buffer).sendToTarget(); //readMessageX por buffer              
 } catch (IOException e) {Log.e(TAG, "disconnected", e);connectionLost();break; }          
            }//_____________-----FIN DE WHILE (TRUE)-----_________________
       
         }//**************_________FIN DE PUBLIC VOID RUN__________**************
//<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>     
        
    }//--------------------*****FIN de la clase ConnectedThread*****-------------------
    
       
} //<<<<<____>>>>>_____<<<<<_____>>>>>_____FIN DE LA CLASE CONEXIONBT_____<<<<<_____>>>>>____<<<<<_____>>>>>    
