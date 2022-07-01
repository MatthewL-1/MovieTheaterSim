# MovieTheaterSim
Used Java to create a simulation for a movie theater that utilizes threading and semaphores (completed in May 2022)

To Run:
1. navigate to location of program and txt file in terminal. type: "javac Project2.java" then hit enter to compile program
2. next type "java Project2" hit enter and it should run
3. While I was running the project I kept the textfile in the same folder as the java file. 
    - So if you do not have the movies.txt file in the same folder, then you will need to add the file path to line 334 to run.

## Purpose

To gain a better understanding of thread concurrency along with thread synchonization and communication in Java with the use of threads and semaphores.
This was accomplished by creating a simulation of a movie theater that simulates customers, and multiple types of employees from the theater that all need to interact with the use of threading.
The threads interact in a given work flow, where once a customer thread buys a ticket they then need to interact with the ticket taker and concessions worker threads if they want snacks. 

