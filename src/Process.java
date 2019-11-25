import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Process {
    
    private static String URL_PREFIX = "rmi://localhost/process%d"; 
    public static void main(String[] args) {
        int numProcesses = 0;

        if( args[1].isEmpty()){
            System.out.println("Uso: java Process <num_processes>");
            System.exit(1);
        }

        numProcesses = Integer.parseInt(args[1]);
        String[] urls = new String[numProcesses];
        
        for (int i = 0; i < urls.length; i++) {
            urls[i] = String.format(URL_PREFIX, numProcesses);
        }
        //RMI init
        try{
            
            LocateRegistry.createRegistry(1099);
        } catch(RemoteException e){
            e.printStackTrace();
        }

    }

}