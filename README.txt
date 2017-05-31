SYSC 3303 Project Iteration 4

Members: 
Matthew Penner
Julia St-Jean
Benjamin Buttera
Alexandre Botelho
Paul Seguin


Setup Instruction
Import the included Java source files to a new project in Eclipse.
As well, test files should be placed in Client and Server folders in the directory from which the project will be run. (ex: project/Client/test.txt)

Included Files:
1. Client
- Server and Host must be running before transfers are started for it to work
- Options are printed to the console
- Gives the option to toggle Test mode, Verbose mode, quit, change the directory files are saved to/read from, or change the  target IP by typing the corresponding key (in the console)
- Gives the option to start a read or write request by typing the corresponding key (in the console)

2. Host
- This is the intermediate host/ error simulator
- Once running, if the client and server are in test mode, it waits for a packet from the client, forwards it to the server
- Then it waits for a packet from the server and forwards it to the client
- Can simulate errors from the test cases (options can be found by typing 'E' into the Host console)
- Server and Client must be running before the host can forward packets

3. Server
- Gives the option to toggle Verbose mode, quit or change the directory files are saved to/read from
- Sends messages back to the client through the host
- Can either read data from a file or write data to a file depending on request


How to run:
1. Run Server as Java application
2. Run Host as Java application
3. Run Client as Java application
4. Data is printed to the console

To simulate Error Code 4 or 5:
1. Ensure Client and Server are running on test mode
2. Type 'E' into the Host console to see possible test cases
3. Type the corresponding letter into the Host console


Breakdown of responsibilities:
 - Coding: Matthew Penner, Julia St-Jean
 - Diagrams: Benjamin Buttera, Alexandre Botelho, Paul Seguin
 - Debugging: Benjamin Buttera, Alexandre Botelho, Paul Seguin, Julia St-Jean, Matthew Penner
