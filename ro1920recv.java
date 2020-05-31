import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

public class ro1920recv {

private static int listen_port = 9000;
private static DatagramSocket Socket;
private static String output_file = "";
private static int numero_secuencia_serv = 1;

public static void main(String args[]) throws IOException {

    if(args.length != 2){
        
        System.out.println("Argumentos incorrectos. El orden correcto es: ro1920recv output_file listen_port");
        return;
    }

    output_file = args[0].trim();
    listen_port = Integer.parseInt(args[1]);
    
    byte[] buffer_recepcion= new byte[1472];

    System.out.println("Iniciando Servidor...");

    try {
        Socket = new DatagramSocket(listen_port);
    } catch (IOException e1) {
        e1.printStackTrace();
    }

        File Fichero_Salida = new File(output_file);
        FileOutputStream Escribe = new FileOutputStream(Fichero_Salida,false);

    while(true){

        int Enviar_ACK = 1;
        DatagramPacket recepcion = new DatagramPacket(buffer_recepcion,buffer_recepcion.length);
    
        Socket.receive(recepcion);

        if(Enviar_ACK == 1){

            int puerto_Router = recepcion.getPort();
            InetAddress direccion_Router = recepcion.getAddress();

            byte[] cont_paquete = new byte[1472];
            cont_paquete = recepcion.getData();

            byte[] cab_dest_ip_byte = new byte[4];
            cab_dest_ip_byte = Arrays.copyOfRange(cont_paquete,0,4);

            byte[] cab_dest_port_byte = new byte[2];
            cab_dest_port_byte = Arrays.copyOfRange(cont_paquete,4,6); 
            Short dest_port = ByteArrayAShort(cab_dest_port_byte); 

            byte fin_fichero_byte = cont_paquete[6];
            int fin_fichero = (fin_fichero_byte & 0xFF);

            //////////////////////////////////////////////////

            byte[] sello_temporal_byte = new byte[8];
            sello_temporal_byte = Arrays.copyOfRange(cont_paquete,7,15);
            long sello_temporal = ByteArrayALong(sello_temporal_byte);

            long time_stamp = Calendar.getInstance().getTimeInMillis();
        
            long tiempo_de_envio = time_stamp - sello_temporal +3; 
            
            /////////////////////////////////////////////////

            byte[] numero_secuencia_byte = new byte[4];
            numero_secuencia_byte = Arrays.copyOfRange(cont_paquete,15,19);
            int numero_secuencia = ByteArrayAInt(numero_secuencia_byte);

            byte[] size_datos_byte = new byte[2];
            size_datos_byte = Arrays.copyOfRange(cont_paquete,19,21);
            short size_datos = ByteArrayAShort(size_datos_byte);

            byte[] payload = new byte[(int) size_datos];
            System.arraycopy(cont_paquete,21,payload,0,(int) size_datos); 

            if(numero_secuencia != numero_secuencia_serv && fin_fichero == 0){
             
                Escribe.write(payload);
            }

            numero_secuencia_serv = numero_secuencia;
          
            ////////ACK/////////

            byte[] buffer_ACK = new byte[22];
            byte[] tiempo_ack_byte = new byte[8];
            byte[] tiempo_envio_byte = new byte[8];

            /////////////////////////////////////////////////

            long time_stamp_servidor = Calendar.getInstance().getTimeInMillis();
            
            long tiempo_ack = time_stamp_servidor;

            tiempo_ack_byte = longAByteArray(tiempo_ack);
            tiempo_envio_byte = longAByteArray(tiempo_de_envio);

            ///////////////////////////////////////////////////

            ByteBuffer buff = ByteBuffer.wrap(buffer_ACK);
            buff.put(cab_dest_ip_byte);
            buff.put(cab_dest_port_byte);
            buff.put(tiempo_ack_byte);
            buff.put(tiempo_envio_byte);

            DatagramPacket ACK = new DatagramPacket(buffer_ACK,buffer_ACK.length,direccion_Router,puerto_Router);

            try{
                Socket.send(ACK);
            }catch(IOException e){
                System.out.println(e);
            }
            
            if(fin_fichero == 1){
                Escribe.close();
                Disconnect();
                System.out.println("Paramos de escribir");
            }
        }
    }
}

public static void Disconnect(){

    System.out.println("Acaba");
    Socket.close();
    System.exit(0);

}

public static byte[] longAByteArray(long a){

    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.putLong(a);
    return buffer.array();
} 

public static byte[] intAByteArray(int a) {

    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.putInt(a);
    return buffer.array();
} 

public static int ByteArrayAInt(byte[] b){

    ByteBuffer buffer = ByteBuffer.wrap(b);
    return buffer.getInt();
}   

public static long ByteArrayALong(byte[] b){

    ByteBuffer buffer = ByteBuffer.wrap(b);
    return buffer.getLong();
}   

public static String ByteArrayAString(byte[] b){

    ByteBuffer buffer = ByteBuffer.wrap(b);
    String Sbuffer = new String(buffer.array());
    return Sbuffer;
}

public static Short ByteArrayAShort(byte[] a){

    ByteBuffer buffer = ByteBuffer.wrap(a);
    return buffer.getShort();

}

}

