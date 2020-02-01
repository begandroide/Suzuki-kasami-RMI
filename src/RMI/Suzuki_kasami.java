package RMI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
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
        *
        */
        private static final long serialVersionUID = 4466469363475602035L;

        /**
         * A delay between checks of token acquisition.
         */
        private static final int TOKEN_WAIT_DELAY = 10;

        // for every process the sequence number of the last request this process knows
        // about.
        private List<Integer> RN;

        /**
         * Index of a current process
         */
        private int index;

        /**
         * Number of processes participating in message exchange
         */
        private int numProcesses;

        /**
         * URLs of processes in a system
         */
        private String[] urls;

        private Token token = null;
        private int capacity = 0;
        private long velocity = 1;
        private boolean inCriticalSection = false;
        private long cooling = 0;

        /**
         * Estado del proceso
         */
        private ProcessState processState;
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
                this.cooling = (long) ((double) capacity / (2.0 * velocity)) * 1000;
                this.velocity = (long) ((1. / velocity) * 1000);
                System.out.println("Velocidad: " + this.velocity + " -- Enfriamiento: " + this.cooling);
                reset();
                printStatus();
        }

        private void printStatus() {
                System.out.println("\u001B[1mEstado: " + processState.toString() + "\u001B[0m");
        }

        public void reset() {

                this.RN = new ArrayList<Integer>(numProcesses);
                for (int i = 0; i < numProcesses; i++) {
                        RN.add(0);
                }
                processState = new ProcessState();

                token = null;
                inCriticalSection = false;
        }

        public void extract() throws RemoteException {
                // de seguro tenemos token
                int saca = Math.min(token.getCharactersRemaining(), capacity);
                System.out.println("Extracción:");

                String line = "";
                char[] readed = new char[saca];
                List<String> lines = null;
                try (
                        BufferedReader bReader = new BufferedReader(new FileReader(token.getFileName()));
                ) {
                        // sacar primera linea
                        Path path = FileSystems.getDefault().getPath("", token.getFileName());
                        System.out.println(path);
                        lines = Files.readAllLines(path , StandardCharsets.UTF_8);
                        System.out.println(lines.size());
                        if ((line = bReader.readLine()) != null) {
                                readed = line.substring(0, saca).toCharArray();
                        }
                        bReader.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }

                try(
                        BufferedWriter bWriter = new BufferedWriter(new FileWriter(token.getFileName()));
                ){
                        // lines.remove(0);
                        lines.set(0, line.substring(saca,line.length()));
                        for (String string : lines) {
                                bWriter.write(string + "\n");
                        }
                        bWriter.close();
                } catch(IOException e){
                }

                for (int i = 0; i < saca; i++) {
                        System.out.print(token.readCharacter());
                        System.out.print(readed[i] + "\u001B[0m" );
                        try {
                                Thread.sleep(velocity);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                if (saca > 0) {
                        System.out.println();
                }
        }

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

        private void giveToken(String url){
                Suzuki_kasami_rmi dest;
                try {
                        dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                        dest.takeToken(token);
                } catch (MalformedURLException | RemoteException | NotBoundException e) {
                        e.printStackTrace();
                }
                token = null;

                processState.status = Status.IDLE;
                printStatus();
        }

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
                // broadcast request
                RN.set(index, RN.get(index) + 1);
                
                for (String url : urls) {
                        Suzuki_kasami_rmi dest;
                        try {
                                dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                                dest.request(index, RN.get(index));
                        } catch (MalformedURLException |NotBoundException | RemoteException e) {
                        }
                }
                if(token != null) {
                        inCriticalSection = true;
                        this.token = (Token) token;
                        processState.status = Status.CRITICALSECTION;
                        printStatus();
                } else{
                        waitToken();
                }
                if( killed == false ){
                        extract();
                        try {
                                leaveToken();
                        } catch (MalformedURLException | NotBoundException e) {
                                e.printStackTrace();
                        }
                }

        }

        public void leaveToken() throws MalformedURLException, NotBoundException, RemoteException {
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
                } else{
                        //no quedan recursos, matamos a los procesos
                        System.out.println("Proceso index " + index + " bajando a los otros");
                        for (String uri : urls) {
                                if(!uri.contains(String.valueOf(index))){
                                        dest = (Suzuki_kasami_rmi) Naming.lookup(uri);
                                        dest.kill();
                                }
                        }
                        kill();
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
}