package RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Suzuki_kasami_rmi extends Remote {

        /**
         * Petición de ingresar a extracción de letras
         * 
         * @param token token, objeto único, puede ser pasado como argumento
         * en el caso de que el proceso posea el token al inicio del algoritmo.
         * @throws RemoteException
         */
        public void initializeExtractProcess(Token token) throws RemoteException;

        /**
         * registra una peticion (request) de un proceso (remoto)
         * 
         * @param id  id del proceso que hace la petición
         * @param seq número de petición del proceso
         * @throws RemoteException
         */
        public void request(int id, int seq) throws RemoteException;

        /**
         * le indica a un proceso remoto que debe esperar por el token para realizar la
         * sección crítica
         * 
         * @throws RemoteException
         */
        public void waitToken() throws RemoteException;

        /**
         * Toma posesión del token en el proceso
         * 
         * @param token objeto token
         * @throws RemoteException
         */
        public void takeToken(Token token) throws RemoteException;

        /**
         * Mata al proceso remoto. Se debe usar para detener el algoritmo S-K una vez
         * que el token haya pasado por todos los nodos del sistema
         * 
         * @throws RemoteException
         */
        public void kill() throws RemoteException;

}