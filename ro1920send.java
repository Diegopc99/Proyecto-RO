import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;

public class ro1920send {

    private static short dest_port = 9000;
    private static String dest_IP = "";
    private static String input_file = "";
    private static String emulator_IP = "";
    private static short emulator_port = 10000;
    private static FileInputStream in = null;
    private static byte Uno = 1;
    private static byte Cero = 0;
    private static int numero_secuencia = 0;
    private static int k = 1;
    private static int RTO = 200; 
    private static long Timer_inicio_ejecucion = 0;
    private static long Timer_fin_ejecucion = 0;
    private static double rtt_medio = 0 ; 
    private static double desviacion_media = 0;
    private static double rtt_medio_anterior = 0; 
    private static double desviacion_media_anterior = 0;
    private static double rtt_actual = 200;
    private static DatagramSocket Socket = null;

    public static void main(String args[]){

    if(args.length != 5){
        
        System.out.println("Argumentos incorrectos. El orden correcto es: ro1920send input_file dest_IP dest_port emulator_IP emulator_port");
        return;
    }

    input_file = args[0].trim();
    dest_port = Short.parseShort(args[2].trim());
    emulator_IP = args[3].trim();
    emulator_port = Short.parseShort(args[4].trim());
    dest_IP = args[1].trim();

    Timer_inicio_ejecucion = Calendar.getInstance().getTimeInMillis();

    byte[] cab_dest_IP = new byte[4];// Cuatro primeros de la cab de aplicacion
    byte[] cab_dest_port = new byte[2];// Dos ultimos cabecera aplicacion
    byte fin_fichero = Cero; //Vale 1 si acaba el fichero
    byte[] sello_temporal = new byte[8]; 
    byte[] numero_secuencia_byte = new byte[4];
    byte[] size_datos = new byte[2];
    
    try {
        cab_dest_IP = (InetAddress.getByName(dest_IP)).getAddress();
    } catch (UnknownHostException e1) {
        e1.printStackTrace();
    } 
 
    cab_dest_port = ShortAByteArray(dest_port);

    try{
        in = new FileInputStream(input_file);
    }catch(IOException e){
        System.out.println(e);
    }

    int n = 0;
    byte[] buffer = new byte[1451];

    try {
        Socket = new DatagramSocket();
    } catch (SocketException e1) {
        e1.printStackTrace();
    }
            

    try {
        while ((n = in.read(buffer)) != -1) {

            byte[] allByteArray = new byte[1472];

            size_datos = ShortAByteArray((short) n);

            ByteBuffer buff = ByteBuffer.wrap(allByteArray);
            buff.put(cab_dest_IP);
            buff.put(cab_dest_port);
            buff.put(fin_fichero);
            buff.put(sello_temporal);
            buff.put(numero_secuencia_byte);
            buff.put(size_datos);
            buff.put(buffer);
            byte[] buffer_combined = new byte[1472];
            buffer_combined = buff.array();

            ///////////////////////////////////////////
            
            boolean ACK_recibido = true;

            do {

                try {

                    ACK_recibido = true;

                    InetAddress Direccion = InetAddress.getByName(emulator_IP);

                    Socket.setSoTimeout((int)RTO);

                    long time_stamp = Calendar.getInstance().getTimeInMillis();
                    sello_temporal = longAByteArray(time_stamp);

                    numero_secuencia_byte = intAByteArray(numero_secuencia);    

                    System.arraycopy(numero_secuencia_byte,0,buffer_combined,15,4); 
                    System.arraycopy(sello_temporal, 0, buffer_combined, 7, 8); 

                    DatagramPacket envio = new DatagramPacket(buffer_combined, buffer_combined.length, Direccion,
                            emulator_port);
                    Socket.send(envio);

                    //////////////////////////////////// ACK ////////////////////////////////////

                    byte[] buffer_recepcion = new byte[22];
                    byte[] recibido = new byte[22];

                    DatagramPacket recepcion = new DatagramPacket(buffer_recepcion, buffer_recepcion.length);
                    Socket.receive(recepcion);

                    recibido = recepcion.getData();

                    if(numero_secuencia ==1){
                        numero_secuencia = 0;
                    }else if(numero_secuencia == 0){
                        numero_secuencia = 1;
                    }

                    byte[] tiempo_envio_byte = new byte[8];
                    tiempo_envio_byte = Arrays.copyOfRange(recibido,14,22);
                    long tiempo_envio = ByteArrayALong(tiempo_envio_byte);
        
                    rtt_actual = (int) ((tiempo_envio*2));

                    ////////////////////////////////RTO//////////////////////////////////

                    /////////////// Smoothed Round-Trip Time //////////////////////////////
                                    
                    if(k == 1){
                        rtt_medio = rtt_actual;
                    }else{
                        rtt_medio =(0.875)* rtt_medio_anterior + (0.125)*rtt_actual;
                    }

                    //////////////// Desviacion del RTT //////////////////////////////

                    if(k == 1){
                        desviacion_media = (rtt_actual/2);
                    }else{
                        desviacion_media =(((0.75)*desviacion_media_anterior) + ((0.25)* (Math.abs(rtt_medio-rtt_actual))));
                    }

                    ///////////////// Temporizador de Retransmision /////////////////////
                    
                    RTO = (int)(rtt_medio + (4*desviacion_media));
                    k++;

                    rtt_medio_anterior = rtt_medio;
                    desviacion_media_anterior = desviacion_media;

                    ///////////////////////////////////////////////////////

                } catch (SocketTimeoutException e) {
                    ACK_recibido = false;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            } while (ACK_recibido == false);

        }

            byte[] allByteArray = new byte[1472];

            fin_fichero = Uno;
            byte[] buffer_vacio = new byte[1451];

             ByteBuffer buff = ByteBuffer.wrap(allByteArray);
             buff.put(cab_dest_IP);
             buff.put(cab_dest_port);
             buff.put(fin_fichero);
             buff.put(sello_temporal);
             buff.put(numero_secuencia_byte);
             buff.put(buffer_vacio);
             byte[] buffer_combined = new byte[1472];
             buffer_combined = buff.array();

             boolean ACK_recibido = true;
             InetAddress Direccion = InetAddress.getByName(emulator_IP);

             do{

                ACK_recibido = true;
                DatagramPacket envio = new DatagramPacket(buffer_combined, buffer_combined.length, Direccion,
                            emulator_port);

                try{
                    Socket.send(envio);
                }catch(SocketException w){
                    ACK_recibido = true;
                }
                    byte[] buffer_recepcion = new byte[22];

                    DatagramPacket recepcion = new DatagramPacket(buffer_recepcion, buffer_recepcion.length);

                try{
                    Socket.receive(recepcion);
                    System.out.println("ACK recibido");
                }catch(SocketTimeoutException e) {
                    ACK_recibido = false;
                }
                
             }while(ACK_recibido == false);
    
             Socket.close();

    } catch (IOException e) {
        e.printStackTrace();
    }

    Timer_fin_ejecucion = Calendar.getInstance().getTimeInMillis();

    long Tiempo_ejecucion = 0;
    Tiempo_ejecucion = Timer_fin_ejecucion - Timer_inicio_ejecucion;
    Tiempo_ejecucion = Tiempo_ejecucion / 1000;
    System.out.println("Tiempo de ejecucion: " + Tiempo_ejecucion);
    
}

public static byte[] intAByteArray(int a){

    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.putInt(a);
    return buffer.array();
} 

public static byte[] longAByteArray(long a){

    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(a);
    return buffer.array();
} 

public static long ByteArrayALong(byte[] b){

    ByteBuffer buffer = ByteBuffer.wrap(b);
    return buffer.getLong();
}   

public static int ByteArrayAInt(byte[] b){

    ByteBuffer buffer = ByteBuffer.wrap(b);
    return buffer.getInt();
}   
public static byte[] StringAByteArray(String a){

    ByteBuffer buffer = ByteBuffer.wrap(a.getBytes(StandardCharsets.UTF_8));
    return buffer.array();
}

public static byte[] ShortAByteArray(short a){

    ByteBuffer buffer = ByteBuffer.allocate(2);
    buffer.putShort(a);
    return buffer.array();

}

}
