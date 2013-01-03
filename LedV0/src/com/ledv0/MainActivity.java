package com.ledv0;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;


@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
	// Debugging
    public static final String TAG = "LEDv0";
    public static final boolean D = true;
    // Tipos de mensaje enviados y recibidos desde el Handler de ConexionBT
    public static final int Mensaje_Estado_Cambiado = 1;
    public static final int Mensaje_Leido = 2;
    public static final int Mensaje_Escrito = 3;
    public static final int Mensaje_Nombre_Dispositivo = 4;
    public static final int Mensaje_TOAST = 5;	        
    public static final int MESSAGE_Desconectado = 6;
    public static final int REQUEST_ENABLE_BT = 7;
    
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    //Nombre del dispositivo conectado
    private String mConnectedDeviceName = null;
    // Adaptador local Bluetooth 
    private BluetoothAdapter AdaptadorBT = null;    
    //Objeto miembro para el servicio de ConexionBT 
    private ConexionBT Servicio_BT = null;	    
    //Vibrador
    private Vibrator vibrador;    
    //variables para el Menu de conexión
    private boolean seleccionador=false;
    public int Opcion=R.menu.activity_main;    
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
      	final ToggleButton BotonLed = (ToggleButton)findViewById(R.id.Led1); 	 	
     	BotonLed.setOnClickListener(new View.OnClickListener() {
				public void onClick(View vv) {   
					
					if(BotonLed.isChecked())     {
						 if(D) Log.e("BotonLed", "Encendiendo..");
						 sendMessage("1\r");
						 findViewById(R.id.Led1).setBackgroundResource(R.drawable.ledcamaleon2);						 
					}	                	
					else {
						 if(D) Log.e("BotonLed", "Apagando..");
						 sendMessage("2\r");
						 findViewById(R.id.Led1).setBackgroundResource(R.drawable.ledcamaleon1);
					}	                            
				}
			});//fin de metodo de BotonLed
        
    }

    public  void onStart() {
    super.onStart();
    ConfigBT();
    }
    
    @Override
    public void onDestroy(){
    	 super.onDestroy();
    	 if (Servicio_BT != null) Servicio_BT.stop();//Detenemos servicio
    }
      
    public void ConfigBT(){
    	// Obtenemos el adaptador de bluetooth
        AdaptadorBT = BluetoothAdapter.getDefaultAdapter();
        if (AdaptadorBT.isEnabled()) {//Si el BT esta encendido,
       	 if (Servicio_BT == null) {//y el Servicio_BT es nulo, invocamos el Servicio_BT    		 
       		 Servicio_BT = new ConexionBT(this, mHandler);
       	 }      
        }
   	 else{ if(D) Log.e("Setup", "Bluetooth apagado...");
     Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
     startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT); 
   	 }
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
  	  //Una vez que se ha realizado una actividad regresa un "resultado"...
  	      switch (requestCode) {
  	               
  	            case REQUEST_ENABLE_BT://Respuesta de intento de encendido de BT
  	                if (resultCode == Activity.RESULT_OK) {//BT esta activado,iniciamos servicio
  	                	 ConfigBT();
  	                } else {//No se activo BT, salimos de la app                 
  	                    finish();}
  	                
  	                
  	                
  	                }//fin de switch case
  	            }//fin de onActivityResult     
  	        
  	       
        
    @Override
    public boolean onPrepareOptionsMenu(Menu menux){
    //cada vez que se presiona la tecla menu  este metodo es llamado
    menux.clear();//limpiamos menu actual    
        	if (seleccionador==false)Opcion=R.menu.activity_main;//dependiendo las necesidades
        	if (seleccionador==true)Opcion=R.menu.desconecta;  // crearemos un menu diferente
   	getMenuInflater().inflate(Opcion, menux);
    return super.onPrepareOptionsMenu(menux);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.Conexion:
        	if(D) Log.e("conexion", "conectandonos");        	     
	                 vibrador = (Vibrator) getSystemService(VIBRATOR_SERVICE);
	                 vibrador.vibrate(1000);	 
	                 String address = "00:06:66:42:A7:C5";//Direccion Mac del  rn42                     
	                 BluetoothDevice device = AdaptadorBT.getRemoteDevice(address);
	                 Servicio_BT.connect(device);         		 	        		              	
            return true;	    
            
        case R.id.desconexion:
        	 if (Servicio_BT != null) Servicio_BT.stop();//Detenemos servicio
        	 return true;
        }//fin de swtich de opciones
        return false;
    }//fin de metodo onOptionsItemSelected
   
    

    public  void sendMessage(String message) {
        if (Servicio_BT.getState() == ConexionBT.STATE_CONNECTED) {//checa si estamos conectados a BT   
        if (message.length() > 0) {   // checa si hay algo que enviar
            byte[] send = message.getBytes();//Obtenemos bytes del mensaje
            if(D) Log.e(TAG, "Mensaje enviado:"+ message);            
                 Servicio_BT.write(send);     //Mandamos a escribir el mensaje     
        		}
        	} else Toast.makeText(this, "No conectado", Toast.LENGTH_SHORT).show();
    		}//fin de sendMessage
    
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
	            	
	                switch (msg.what) {
	     //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>   
	                case Mensaje_Escrito:
	                	  byte[] writeBuf = (byte[]) msg.obj;//buffer de escritura...
	                      // Construye un String del Buffer
	                      String writeMessage = new String(writeBuf);
	                      if(D) Log.e(TAG, "Message_write  =w= "+ writeMessage);  
	                    break;
	      //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>                                
	                case Mensaje_Leido:   	                 
	                	byte[] readBuf = (byte[]) msg.obj;//buffer de lectura...
	                    //Construye un String de los bytes validos en el buffer
	                  String readMessage = new String(readBuf, 0, msg.arg1);
	                  if(D) Log.e(TAG, "Message_read   =w= "+ readMessage);              	                 
	                    break;
	     //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>	           
	                case Mensaje_Nombre_Dispositivo:
	                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME); //Guardamos nombre del dispositivo
	     Toast.makeText(getApplicationContext(), "Conectado con "+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	     seleccionador=true;
	                    break;
	    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>                  
	                case Mensaje_TOAST:
	                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                    Toast.LENGTH_SHORT).show();
	                    break;
	     //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 
	                case MESSAGE_Desconectado:	
	                  	 if(D) Log.e("Conexion","DESConectados");
	                  	 seleccionador=false;             	
	          break;
	    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  
	                }//FIN DE SWITCH CASE PRIMARIO DEL HANDLER
	            }//FIN DE METODO INTERNO handleMessage
	        };//Fin de Handler
    
       
}//Fin MainActivity






















