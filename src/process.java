
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import RMI.Suzuki_kasami;
import RMI.Suzuki_kasami_rmi;
import RMI.Token;

public class process {

        private static String URL_PREFIX = "rmi://localhost/process%d";
        private static String URL_REGEX = "^process([0-9]+)$";

        static int numProcesses;
        static int indexProcess;
        static String fileName;
        static int capacity;
        static int velocity;
        static int delay;
        static boolean bearer;
        static String[] urls;
        static boolean creatorRegistry;
        static Registry registry = null;

        public static void main(String[] args) {

                if (args.length < 6) {
                        System.out.println("Uso: java process <num_processes> <file_name> <capacity> <velocity> <delay> <bearer>");
                        System.exit(1);
                }

                parseArgs(args);

                for (int i = 0; i < urls.length; i++) {
                        urls[i] = String.format(URL_PREFIX, i);
                }

                initRmiRegistry();
               
                getIndexByRegisterListed();

                Suzuki_kasami_rmi process = null;

                try {
                        process = new Suzuki_kasami(urls, indexProcess, capacity, velocity);
                        Naming.bind(urls[indexProcess], process);
                } catch (RemoteException | MalformedURLException | AlreadyBoundException e) {
                        try {
                                Naming.rebind(urls[indexProcess], process);
                        } catch (RemoteException | MalformedURLException e1) {
                                e1.printStackTrace();
                        }
                }

                Token token = null;
                if (bearer) {
                        System.out.println("Instanciando Token");
                        token = new Token(numProcesses, capacity, fileName);
                }

                //esperamos delay, para inicializar el proceso extractor
                try {
                        Thread.sleep(delay);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }

                //iniciamos el proceso extractor
                try {
                        process.initializeExtractProcess(token);
                } catch (RemoteException e) {
                        e.printStackTrace();
                }

                //si es el creador del rmi registro, debe esperar a que terminen los demás
                //para evitar caer en interrupciones.
                if(creatorRegistry){
                        System.out.println("Esperando término de otros procesos para bajar registro rmi");
                        try {
                                Thread.sleep(1500);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                }
                System.out.println("Proceso de extracción terminado");
                System.exit(0);
        }

        /**
         * Obtener un id y url para el proceso actual, según los registros rmi
         * ya ligados en el algoritmo.
         */
        private static void getIndexByRegisterListed() {
                String[] endPoints = null;
                try {
                        endPoints = registry.list();
                } catch (RemoteException e) {
                        e.printStackTrace();
                }

                if (endPoints.length > 0) {
                        String endString = endPoints[endPoints.length - 1];
                        Pattern pattern = Pattern.compile(URL_REGEX);

                        Matcher matcher = pattern.matcher(endString);
                        if (matcher.matches()) {
                                indexProcess = Integer.valueOf(matcher.group(1)) + 1;
                        } else {
                                System.out.println("ha ocurrido un problema al ligar el url rmi");
                                System.exit(1);
                        }

                }
        }

        /**
         * Método para inicializar registro rmi en puerto 1099, si ya esta creado, se
         * salta el método.
         */
        private static void initRmiRegistry() {
                // RMI init
                try {
                        LocateRegistry.createRegistry(1099);
                } catch (RemoteException e2) {
                        creatorRegistry = false;
                        System.out.println("registro rmi ya estaba creado, skip");
                }

                try {
                        registry = LocateRegistry.getRegistry();
                } catch (RemoteException e) {
                        e.printStackTrace();
                }
        }

        /**
         * Método para parsear argumentos del programa
         * 
         * @param args Arreglo de String con propiedades.
         */
        private static void parseArgs(String[] args) {
                numProcesses = Integer.parseInt(args[0]);
                fileName = args[1];
                capacity = Integer.parseInt(args[2]);
                velocity = Integer.parseInt(args[3]);
                delay = Integer.parseInt(args[4]);
                bearer = ( (args[5].compareTo("True") == 0 || args[5].compareTo("true") == 0 ) ? true : false);
                urls = new String[numProcesses];
                creatorRegistry = true;
        }

}