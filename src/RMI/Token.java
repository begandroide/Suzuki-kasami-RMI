package RMI;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

public class Token implements Serializable {

        private static final long serialVersionUID = 20120731125400L;

        private Queue<Integer> queue;
        private int[] ln;
        private int capacity;
        private Boolean isInstantiated = false;
        // private String  contentFile = "";
        private String fileName = "";
        private int charactersRemaining;
        private  int initialCharacters;

        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_RED = "\u001B[31m";
        public static final String ANSI_GREEN = "\u001B[32m";
        public static final String ANSI_YELLOW = "\u001B[33m";
        public static final String ANSI_BLUE = "\u001B[34m";
        public static final String ANSI_BOLD = "\u001B[1m";

        public Token(int numProcesses, int inCapacity, String fileName) {
                if (!isInstantiated) {

                        isInstantiated = true;
                        capacity = inCapacity;
                        this.fileName = fileName;
                        try (
                                BufferedReader bReader = new BufferedReader(new FileReader(fileName));
                        ) {
                                // saber cantidad de recurso (letras) disponibles
                                charactersRemaining = countInitialResources(bReader);
                                bReader.close();
                        } catch (FileNotFoundException e) {
                                e.printStackTrace();
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                        System.out.println("recurso inicial:" + charactersRemaining);
                        queue = new LinkedList<Integer>();
                        ln = new int[numProcesses];
                        for (int i = 0; i < numProcesses; i++) {
                                ln[i] = 0;
                        }       
                }
        }

        /**
         * Contar recurso inicial
         * @param reader
         * @return
         * @throws IOException
         */
        private int countInitialResources(BufferedReader reader) throws IOException {
                String data = "";
                while ((data = reader.readLine()) != null) {
                        charactersRemaining += data.length();
                }
                initialCharacters = charactersRemaining;
                return charactersRemaining;
        }

        /**
         * Obtener numero de secuencia LN[position] del proceso
         * 
         * @param index posicion del proceso
         * @return numero de secuencia registrado en LN
         */
        public int getLni(int index) {
                return ln[index];
        }

        /**
         * Registrar numero de secuencia en LN del proceso
         * 
         * @param index indice en LN
         * @param value valor a registrar
         */
        public void setLni(int index, int value) {
                ln[index] = value;
        }

        /**
         * Verificar si Cola contiene al proceso id
         * 
         * @param id id del proceso a verificar
         * @return Si el id del proceso está en la cola o no.
         */
        public Boolean queueContains(int id) {
                return queue.contains(id);
        }

        /**
         * Verificar si Cola esta vacía
         * 
         * @return
         */
        public Boolean queueIsEmpty() {
                return queue.isEmpty();
        }

        /**
         * Añadir id de proceso a la Cola
         * 
         * @param id id del proceso
         */
        public void addId(int id) {
                queue.add(id);
        }

        /**
         * Remover elemento en cabeza de la Cola
         * 
         * @return id del proceso removido
         */
        public int popId() {
                return queue.remove();
        }

        /**
         * Obtener la capacidad de extracción permitido
         * 
         * @return capacidad
         */
        public int getCapacity() {
                return capacity;
        }

        /**
         * Obtener la cantidad de caracteres restantes
         * 
         * @return capacidad
         */
        public int getCharactersRemaining() {
                return charactersRemaining;
        }

        /**
         * Obtener el nombre del archivo
         * 
         * @return nombre del archivo
         */
        public String getFileName() {
                return fileName;
        }

        /**
         * Obtener color que debe escribirse en consola,
         * según cantidad de recurso disponible
         * @return
         */
        private String getColor(){
                
                double percent = 100*((double)charactersRemaining/initialCharacters);
                if(100.0>=percent && percent>=75.0 ){
                        return ANSI_BLUE;
                } 
                if(75.0>=percent && percent>=50.0 ){
                        return ANSI_GREEN;
                }
                if(50.0>=percent && percent>=25.0 ){
                        return ANSI_YELLOW;
                }
                
                return ANSI_RED;
        }

        /**
         * Obtiene color y descuenta cantidad de recurso restante.
         * @return
         */
        public String readCharacter() {

                String readed = getColor() + ANSI_BOLD ;//+ String.valueOf( contentFile.charAt(0) ) + ANSI_RESET ;
                
                if(charactersRemaining > 0){
                        charactersRemaining -= 1;
                }
                return readed;
        }
}