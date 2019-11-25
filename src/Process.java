import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;

import RMI.Suzuki_kasami;
import RMI.Suzuki_kasami_rmi;
import RMI.Token;

public class Process {

    private static String URL_PREFIX = "rmi://localhost/process%d";

    public static void main(String[] args) {
        int numProcesses = 0;

        if (args[0].isEmpty()) {
            System.out.println("Uso: java Process <num_processes> <capacity>");
            System.exit(1);
        }

        numProcesses = Integer.parseInt(args[0]);
        int capacity = Integer.parseInt(args[1]);

        String[] urls = new String[numProcesses];

        for (int i = 0; i < urls.length; i++) {
            urls[i] = String.format(URL_PREFIX, i);
        }
        // RMI init
        try {

            LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        ArrayList<ProcessThread> processes = new ArrayList<ProcessThread>();

        // locate processes
        int processIndex = 0;
        for (String url : urls) {
            Suzuki_kasami_rmi process;

            try {
                process = new Suzuki_kasami(urls, processIndex);
                processIndex++;
                processes.add(new ProcessThread(process));
                try {
                    Naming.bind(url, process);
                } catch (MalformedURLException | AlreadyBoundException e) {
                    try {
                        Naming.rebind(url, process);
                    } catch (MalformedURLException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
        System.out.println("INSTANCIACION TOKEN");
        Token token = Token.instantiate(numProcesses, capacity, "../README.md");

        for (int i = 0; i < urls.length; i++) {
            new Thread(processes.get(i)).start();
        }
        System.out.println("dando token");
        try {
            processes.get(0).process.takeToken(token);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        

    }

}