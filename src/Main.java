
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

public class Main {

        private static String URL_PREFIX = "rmi://localhost/process%d";
        private static String URL_REGEX = "^process([0-9]+)$";

        static Registry registry = null;

        public static void main(String[] args) {

                if (args.length < 5) {
                        System.out.println("Uso: java Main <num_processes> <capacity> <velocity> <delay> <bearer>");
                        System.exit(1);
                }

                int numProcesses = Integer.parseInt(args[0]);
                int capacity = Integer.parseInt(args[1]);
                int velocity = Integer.parseInt(args[2]);
                int delay = Integer.parseInt(args[3]);
                boolean bearer = (args[4].compareTo("True") == 0 ? true : false);
                String[] urls = new String[numProcesses];

                for (int i = 0; i < urls.length; i++) {
                        urls[i] = String.format(URL_PREFIX, i);
                }
                // RMI init
                try {
                        LocateRegistry.createRegistry(1099);
                } catch (RemoteException e2) {
                        System.out.println("registro rmi ya estaba creado, skip");
                }

                try {
                        registry = LocateRegistry.getRegistry();
                } catch (RemoteException e) {
                        e.printStackTrace();
                }

                String[] endPoints = null;
                try {
                        endPoints = registry.list();
                } catch (RemoteException e) {
                        e.printStackTrace();
                }
                int indexProcess = 0;

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
                        token = new Token(numProcesses, capacity, "../testfile");
                }

                try {
                        Thread.sleep(delay);
                } catch (InterruptedException e1) {
                        e1.printStackTrace();
                }

                try {
                        process.initializeExtractProcess(token);
                } catch (RemoteException e) {
                        e.printStackTrace();
                }

                System.out.println("Proceso de extracción terminado");
                System.exit(0);

        }

}