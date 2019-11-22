
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Suzuki_kasami_rmi extends Remote {

    public void request(int id, int seq) throws RemoteException;

    public void waitToken() throws RemoteException;

    public void takeToken() throws RemoteException;

    public void kill() throws RemoteException;

}