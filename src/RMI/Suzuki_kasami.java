package RMI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import RMI.ProcessState.Status;

public class Suzuki_kasami extends UnicastRemoteObject implements Suzuki_kasami_rmi {

        /**
        * Necesario cuando implementamos la interfaz remota
        */
        private static final long serialVersionUID = 4466469363475602035L;


        /**
         * Delay para volver a chequear si tenemos el token.
         * Problema de diseño: bussy waiting
         */
        private static final int TOKEN_WAIT_DELAY = 10;

        /**
         * Arreglo que contiene el número de secuencia de los procesos, donde 
         * RN[j] es el último número de secuencia recibido desde el proceso j. 
         */
        private List<Integer> RN = null;

        /**
         * Indice del proceso actual
         */
        private int index = 0;

        /**
         * Cantidad de procesos participando en el algoritmo
         */
        private int numProcesses = 0;

        /**
         * URLs de procesos en el algoritmo
         */
        private String[] urls = null;

        /**
         * Objeto único en el sistema, el cual autoriza el paso a la 
         * sección crítica. El proceso al obtenerlo, entra a la SC.
         */
        private Token token = null;

        /**
         * Capacidad de extracción de un proceso. Quiere decir
         * la cantidad de letras que se pueden extraer en la sección crítica.
         */
        private int capacity = 0;

        /**
         * Velocidad de extracción de letras. (Cantidad de letras/seg) 
         */
        private long velocity = 1;
        
        /**
         * (Enfriamiento) Tiempo que el proceso espera antes de volver a 
         * pedir el token.
         */
        private long cooling = 0;

        /**
         * Si el proceso está en SC.
         */
        private boolean inCriticalSection = false;

        /**
         * Estado del proceso
         */
        private ProcessState processState = null;

        /**
         * Si el proceso fue bajado o matado.
         */
        private Boolean killed = false;

        /**
         * Default constructor following RMI conventions
         *
         * @param urls  URLs of participating processes
         * @param index index of current process
         * @throws RemoteException if RMI mechanisms fail
         */
        public Suzuki_kasami(String[] urls, int index, int capacity, int velocity) throws RemoteException {
                super();

                this.index = index;
                this.urls = urls;
                this.numProcesses = urls.length;
                this.capacity = capacity;
                this.cooling = (long) ((double) (capacity * 1000) / (2.0 * velocity));
                this.velocity = (long) ((1. / velocity) * 1000);
                System.out.println("Velocidad: " + this.velocity + " -- Enfriamiento: " + this.cooling);
                reset();
                printStatus();
        }

        // begin RMI implementation

        public void request(int id, int seq) throws RemoteException {
                // actualizamos numero de seq del proceso id
                // el maximo entre el RN[id] y seq
                RN.set(id, Math.max(RN.get(id), seq));

                // si no estoy en seccion critica, tengo el token y la petición no es mia
                if (!inCriticalSection && token != null) {
                        if (index != id && (RN.get(id) > token.getLni(id))) {
                                String url = "rmi://localhost/process" + id;
                                int leftCharacts = token.getCharactersRemaining();
                                giveToken(url);
                                reinitialize(leftCharacts);
                        }
                }
        }

        public void waitToken() throws RemoteException {
                processState.status = Status.WAITINGTOKEN;
                printStatus();
                while (token == null && killed == false) {
                        try {
                                Thread.sleep(TOKEN_WAIT_DELAY);
                        } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                        }
                }
        }

        public void takeToken(Token token) throws RemoteException {
                inCriticalSection = true;
                this.token = (Token) token;
                processState.status = Status.CRITICALSECTION;
                printStatus();
        }

        public void kill() throws RemoteException {
                System.out.println("Matando proceso");
                killed = true;
        }

        public void initializeExtractProcess(Token token) throws RemoteException {
                printRN();
                // broadcast petición acceso a SC
                RN.set(index, RN.get(index) + 1);
                //enviamos petición a todos los demas procesos del algoritmo
                for (String url : urls) {
                        Suzuki_kasami_rmi dest;
                        try {
                                dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                                dest.request(index, RN.get(index));
                        } catch (MalformedURLException |NotBoundException  e) {
                        }
                }
                if(token != null) {
                        //si tenemos token, iniciar extracción
                        inCriticalSection = true;
                        this.token = (Token) token;
                        processState.status = Status.CRITICALSECTION;
                        printStatus();
                } else{
                        //esperamos token
                        waitToken();
                }
                if( killed == false ){
                        extract();
                        leaveToken();
                }
        }

        // end RMI implementation

        // begin private implementation

        /**
         * Imprime por consola el estado actual del proceso
         */
        private void printStatus() {
                System.out.println("\u001B[1mEstado: " + processState.toString() + "\u001B[0m");
        }

        /**
         * Reinicializa el proceso, setea en valores iniciales de ejecución de un proceso
         * en el algoritmo.
         */
        private void reset() {
                //re-iniciar arreglo RN
                this.RN = new ArrayList<Integer>(numProcesses);
                for (int i = 0; i < numProcesses; i++) {
                        RN.add(0);
                }
                //Estado idle del proceso
                processState = new ProcessState();
                token = null;
                inCriticalSection = false;
        }

        /**
         * Lee "cantidad" letras del archivo y las elimina del archivo de texto 
         * @param cantidad cantidad de letras a eliminar del principio del archivo
         * @return String con las letras extraidas y removidas del archivo
         */
        private String readWrite(int cantidad){
                String readed = "";
                //lineas del archivo, para eliminar cuando extraemos
                List<String> lines = null;
                try (
                        //buffer para leer letras a extraer
                        BufferedReader bReader = new BufferedReader(new FileReader(token.getFileName()));
                ) {
                        Path path = FileSystems.getDefault().getPath("", token.getFileName());
                        lines = Files.readAllLines(path , StandardCharsets.UTF_8);

                        int index = 0;
                        int sacaTmp = cantidad;
                        while(readed.length() < cantidad){
                                String line = lines.get(index);
                                if(line.length() < sacaTmp){
                                        //si debe leer mas de una linea
                                        readed += line;
                                        sacaTmp -= line.length();
                                        //removermos linea
                                        lines.remove(index);
                                } else{
                                        //si lee menos de una linea
                                        readed += line.substring(0, sacaTmp);
                                        //removemos lo extraido
                                        lines.set(index,line.substring(sacaTmp,line.length()));
                                        index++;
                                }
                        }
                        bReader.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }

                try(
                        BufferedWriter bWriter = new BufferedWriter(new FileWriter(token.getFileName()));
                ){
                        //escribimos en archivo
                        for (int i = 0; i < lines.size(); i++) {
                                if(lines.get(i).length() > 0) {
                                        bWriter.write(lines.get(i));
                                        if(i != lines.size() - 1){
                                                bWriter.write("\n");
                                        }
                                }
                        }
                        bWriter.close();
                } catch(IOException e){
                        e.printStackTrace();
                }

                return readed;
        }

        /**
         * Entrega el token a un proceso remoto
         * @param url url del proceso remoto
         */
        private void giveToken(String url){
                Suzuki_kasami_rmi dest;
                try {
                        //buscamos proceso por lookup
                        dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                        //entregamos el token a destino
                        dest.takeToken(token);
                } catch (MalformedURLException | RemoteException | NotBoundException e) {
                        e.printStackTrace();
                }
                token = null;
                //actualizamos estado de proceso
                processState.status = Status.IDLE;
                printStatus();
        }

        /**
         * Reinicializa la petición del token en el caso de que 
         * queden mas de cero caracteres en el archivo
         * @param leftCharacts cantidad de letras que el proceso actual sabe 
         * que quedan en el token.
         */
        private void reinitialize(int leftCharacts){

                if (leftCharacts > 0) {
                        // solicitar el token para una nueva ronda
                        try {
                                Thread.sleep(cooling);
                                this.initializeExtractProcess(null);
                        } catch (InterruptedException | RemoteException e) {
                                e.printStackTrace();
                        }
                }
        }

        /**
         * Comenzar a extraer letras del archivo, es requerido
         * que el proceso esté dentro de la SC.
         */
        private void extract(){
                // de seguro tenemos token
                // sacamos el minimo entre los caracteres restantes y la capacidad
                int saca = Math.min(token.getCharactersRemaining(), capacity);
                System.out.println("Extracción:");

                // extraemos las letras y actualizamos archivo
                String readed = readWrite(saca);
               
                for (int i = 0; i < saca; i++) {
                        //formato de color dado por token
                        System.out.print(token.readCharacter());
                        System.out.print(readed.charAt(i) + "\u001B[0m" );
                        try {
                                //velocity like sleep
                                Thread.sleep(velocity);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                if (saca > 0) {
                        System.out.println();
                }
        }

        /**
         * Método que deja el token actual, además es el 
         * encargado de enviar el token a otros procesos que 
         * tienen peticiones pendientes.
         */
        private void leaveToken()  {
                //actualizamos LN en token
                token.setLni(index, token.getLni(index) + 1);
                for (int i = 0; i < numProcesses; i++) {
                        if (!token.queueContains(i)) {
                                if (RN.get(i) == (token.getLni(i) + 1)) {
                                        token.addId(i);
                                }
                        }
                }
                // si la cola no esta vacia
                Suzuki_kasami_rmi dest = null;
                int leftCharacts = token.getCharactersRemaining();
                if (!token.queueIsEmpty() && leftCharacts > 0) {
                        // entregamos token
                        int idProcess = token.popId();
                        giveToken("rmi://localhost/process" + idProcess);
                        reinitialize(leftCharacts);
                        inCriticalSection = false;
                } else if(token.queueIsEmpty() && leftCharacts > 0){
                        // si no hay más procesos esperando por token, solicito el token de nuevo
                        reinitialize(leftCharacts);
                        inCriticalSection = false;
                } else {
                        //no quedan recursos, matamos a los procesos
                        System.out.println("Proceso index " + index + " bajando a los otros");
                        for (String uri : urls) {
                                if(!uri.contains(String.valueOf(index))){
                                        try {
                                                dest = (Suzuki_kasami_rmi) Naming.lookup(uri);
                                                dest.kill();
                                        } catch (MalformedURLException | RemoteException | NotBoundException e) {
                                                e.printStackTrace();
                                        }
                                }
                        }
                        try {
                                // se mata el proceso actual
                                kill();
                        } catch (RemoteException e) {
                                e.printStackTrace();
                        }
                }

        }

        /**
         * Imprime el arreglo RN
         */
        private void printRN(){
                System.out.print("\u001B[1mRN => [");
                for (Integer integer : RN) {
                        System.out.print(integer + ",");
                }
                System.out.print("]\u001B[0m\n");
        }

        // end private implementation
}