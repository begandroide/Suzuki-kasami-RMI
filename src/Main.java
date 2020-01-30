
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
                        System.out.println("registro ya estaba creado");
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
                System.out.println("endpoints");
                for (String string : endPoints) {
                        System.out.println(string);
                }
                System.out.println("fin endpoints");
                if (endPoints.length > 0) {
                        String endString = endPoints[endPoints.length - 1];
                        Pattern pattern = Pattern.compile(URL_REGEX);

                        Matcher matcher = pattern.matcher(endString);
                        if (matcher.matches()) {
                                indexProcess = Integer.valueOf(matcher.group(1)) +1;
                                System.out.println("indice actual proceso " + indexProcess);
                        } else {
                                System.out.println("ha ocurrido un problema al ligar el url rmi");
                                System.exit(1);
                        }

                }
                Suzuki_kasami_rmi process = null;

                try {
                        process = new Suzuki_kasami(urls, indexProcess, capacity, velocity);
                        Naming.bind(urls[indexProcess], process);
                        Naming.rebind(urls[indexProcess], process);
                } catch (RemoteException | MalformedURLException | AlreadyBoundException e) {
                        e.printStackTrace();
                }

                Token token = null;
                if(bearer){
                        System.out.println("INSTANCIACION TOKEN");
                        token = new Token(numProcesses, capacity, "../testfile");
                }

                try {
                        System.out.println("thread wait " + delay);
                        Thread.sleep(delay);
                } catch (InterruptedException e1) {
                        e1.printStackTrace();
                }

                try {
                        process.initializeExtractProcess(token);
                } catch (RemoteException e) {
                        e.printStackTrace();
                }
                // try {
                //         process.takeToken(token);
                // } catch (RemoteException e) {
                //         e.printStackTrace();
                // }

                // try {
                //         registry.unbind(urls[indexProcess]);
                //         Naming.unbind(urls[indexProcess]);
                // } catch (MalformedURLException e) {
                //         // TODO Auto-generated catch block
                //         e.printStackTrace();
                // } catch (RemoteException e) {
                //         // TODO Auto-generated catch block
                //         e.printStackTrace();
                // } catch (NotBoundException e) {
                //         // TODO Auto-generated catch block
                //         e.printStackTrace();
                // }

        }

}