# Suzuki-kasami-RMI

Proyecto sistemas distribuidos, se distribuye la extracción de letras de un archivo input, mediante 
procesos distribuidos en maquinas locales o remotas.

[Fuente: Suzuki-kasami algorithm](https://www.geeksforgeeks.org/suzuki-kasami-algorithm-for-mutual-exclusion-in-distributed-system/)

# Instalación

Lo primero es compilar el proyecto, luego debemos inicializar el 
registro rmi para iniciar el servidor rmi.

```bash
begandroide@lab/.../Suzuki-kasami-RMI:~> make default
begandroide@lab/.../Suzuki-kasami-RMI:~> make runrmiregister 
```

Lo anterior dejará los binarios compilados dentro de la carpeta 
*./bin/*.

# Uso

Primero debemos ir a la carpeta *./bin*, y ejecutar la clase **Process**

```bash
begandroide@lab/.../Suzuki-kasami-RMI:~> cd bin/
begandroide@lab/.../Suzuki-kasami-RMI/bin:~> java Process <> <>

```