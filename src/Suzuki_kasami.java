
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Suzuki_kasami extends UnicastRemoteObject implements Suzuki_kasami_rmi, Runnable {

    private Token token = null;

    protected Suzuki_kasami(int index) throws RemoteException {
        super();
        // TODO Auto-generated constructor stub
    }

    public void request(int id, int seq) throws RemoteException {

    }

    public void waitToken() throws RemoteException {

    }

    public void takeToken() throws RemoteException {

    }

    public void kill() throws RemoteException {

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}