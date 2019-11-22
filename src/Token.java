import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

public class Token implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private Queue<Integer> queue;
    private int[] ln;
    private static Boolean isInstantiated = false;

    private Token(int numProcess) {
        queue = new LinkedList<Integer>();
        ln = new int[numProcess];
        for (int i = 0; i < numProcess; i++) {
            ln[i] = 0;
        }
    }

    public static Token instantiate(int numProcesses) {
        if (!isInstantiated) {
            isInstantiated = true;
            return new Token(numProcesses);
        }

        return null;
    }

    public int getLni(int position) {
        return ln[position];
    }

    public void setLni(int position, int value) {
        ln[position] = value;
    }

    public Boolean queueContains(int id) {
        return queue.contains(id);
    }

    public Boolean queueIsEmpty() {
        return queue.isEmpty();
    }

    public void addId(int id) {
        queue.add(id);
    }

    public int popId() {
        return queue.remove();
    }
}