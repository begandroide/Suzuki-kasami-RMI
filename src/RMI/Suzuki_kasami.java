package RMI;

import java.net.MalformedURLException;
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
                this.cooling = (long)(capacity/2.)*1000;
                this.velocity = (long) ((1. / velocity) * 1000);
                System.out.println("VELOCIDAD: " + this.velocity);
                System.out.println("COOLING: " + this.cooling);
                reset();
                printStatus();
        }

        private void printStatus() {
                System.out.println("Proceso " + index + " en estado " + processState.toString());
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
                for (int i = 0; i < saca; i++) {
                        System.out.print(token.readCharacter());
                        try {
                                Thread.sleep(velocity);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                // System.out.println("quedan: " + token.getCharactersRemaining());
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
                                try {
                                        System.out.println("regalando token");
                                        Suzuki_kasami_rmi dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                                        int leftCharacts = token.getCharactersRemaining();
                                        dest.takeToken(token);
                                        token = null;
                                        if (leftCharacts > 0) {
                                                // solicitar el token para una nueva ronda
                                                try {
                                                        Thread.sleep(cooling);
                                                } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                }
                                                this.initializeExtractProcess(null);
                                        }
                                } catch (MalformedURLException | NotBoundException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        }
                }
        }

        public void waitToken() throws RemoteException {
                processState.status = Status.WAITINGTOKEN;
                printStatus();
                while (token == null) {
                        try {
                                Thread.sleep(TOKEN_WAIT_DELAY);
                        } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                        }
                }
        }

        public void takeToken(Token token) throws RemoteException {
                System.out.println("token tomado proceso " + index);
                // System.out.println("token viene con " + token.getCharactersRemaining());
                inCriticalSection = true;
                this.token = (Token) token;
                processState.status = Status.CRITICALSECTION;
                printStatus();
        }

        public void kill() throws RemoteException {
                System.out.println("killing ME");
                try {
                        Naming.unbind(urls[index]);
                } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (NotBoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
                // System.exit(0);
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
                                throw new RuntimeException(e);
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
                extract();
                try {
                        leaveToken();
                } catch (MalformedURLException | NotBoundException e) {
                        e.printStackTrace();
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
                String url = "";
                Suzuki_kasami_rmi dest = null;
                int leftCharacts = token.getCharactersRemaining();
                if (!token.queueIsEmpty()) {
                        int idProcess = token.popId();
                        System.out.println("token se va a :" + idProcess);
                        url = "rmi://localhost/process" + idProcess;
                        dest = (Suzuki_kasami_rmi) Naming.lookup(url);
                        dest.takeToken(token);
                        token = null;

                        processState.status = Status.IDLE;
                        printStatus();
                }

                if (leftCharacts > 0) {
                        // solicitar el token para una nueva ronda
                        try {
                                Thread.sleep(cooling);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                        this.initializeExtractProcess(null);
                } 
                inCriticalSection = false;
        }

        private void printRN(){
                System.out.print("RN => [");
                for (Integer integer : RN) {
                        System.out.print(integer + ",");
                }
                System.out.print("]\n");
        }
}