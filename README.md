# Suzuki-kasami-RMI

Proyecto sistemas distribuidos, se distribuye la extracción de letras de un archivo input, mediante 
procesos distribuidos en maquinas locales o remotas.

[Fuente: Suzuki-kasami algorithm](https://www.geeksforgeeks.org/suzuki-kasami-algorithm-for-mutual-exclusion-in-distributed-system/)

# Instalación

Lo primero es compilar el proyecto:

```bash
begandroide@lab/.../Suzuki-kasami-RMI:~> make default
```

Lo anterior dejará los binarios compilados dentro de la carpeta 
*./bin/*.

# Uso

Primero debemos ir a la carpeta *./bin*, y ejecutar la clase **process**

```bash
begandroide@lab/.../Suzuki-kasami-RMI:~> cd bin/
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process <N_process> <file_name> <capacity> <velocity> <delay> <bearer>
```
Donde 
- N_process: Cantidad de procesos a ser ejecutados
- file_name: Ubicación del archivo que se quiere procesar.
- capacity: Cantidad de letras que puede extraer cada proceso en cada sección crítica
- velocity: Extraemos letra por letra, velocidad es el tiempo de diferencia entre extraer una letra y otra.
- delay: Cuanto esperamos para dar el token desde el thread principal
- bearer: id del proceso que tendrá el token

# Consideración

Se ha añadido una función más al objeto remoto (rmi), la cual es usada para inicializar el proceso de extracción:

```bash
/**
  * Petición de ingresar a extracción de letras
  * 
  * @throws RemoteException
  */
  public void initializeExtractProcess(Token token) throws RemoteException;
```

Se implementa la función anterior debido a que una vez inicializados todos los procesos, hacía falta un método que inicialize el algoritmo;
cabe mencionar que la función **initializeExtractProcess** es un método que encapsula el request del token, la espera del mismo, y maneja el ingreso a la sección crítica. Sin esta función, cada proceso debía hacer 
más operaciones o llamadas al proceso remoto; por lo que nos ayuda con la simplicidad.

# Ejemplo de uso

1) Debemos abrir N_process terminales o consolas para ejecutar el algoritmo.
2) Nos ubicamos en la raíz del proyecto y compilamos con "make default".
3) Luego:

```bash
(Terminal 1)
begandroide@lab/.../Suzuki-kasami-RMI:~> cd bin/
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process 5 ../testfile 1 1 5000 False
```

```bash
(Terminal 2)
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process 5 ../testfile 1 1 5000 True
```

```bash
(Terminal 3)
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process 5 ../testfile 1 1 5000 False
```

```bash
(Terminal 4)
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process 5 ../testfile 1 1 5000 False
```

```bash
(Terminal 5)
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java process 5 ../testfile 1 1 5000 False
```

En el ejemplo anterior, iniciamos 5 procesos, se extraen letras desde el archivo "testfile", que se encuentra
en la raíz del proyecto, le damos una capacidad de 1 letra a cada proceso para extraer en la sección 
crítica, le asignamos una velocidad de 1(letra/segundo), le damos un delay de 5 segundos (5000 milisegundos),
y el proceso 2 tiene el token (bearer = True), 