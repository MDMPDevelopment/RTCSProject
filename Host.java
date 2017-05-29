
import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import java.net.SocketException;

public class Host {
	private static final int receiveLength = 516;
	private static final int NORMAL =0;
	private static final int CHANGEOPCODE = 1;
	private static final int CHANGELENGTH = 2;
	private static final int CHANGETIDSERVER = 3;
	private static final int CHANGETIDCLIENT = 4;
	private static final int CHANGEOPCODEv = 5;
	private static final int DELAYTOSERVER = 6;
	private static final int DELAYTOCLIENT = 7;
	private static final int DUPLICATESERVERPKT = 8;
	private static final int DUPLICATECLIENTPKT = 9;
	private static final int LOSESERVERPKT = 10;
	private static final int LOSECLIENTPKT = 10;
	private boolean  verbose, reset;
	private int targetPort, returnPort, test, errorReq,delay, packetNum, serverPkt, clientPkt;
	private DatagramSocket port23, sndRcvSok, sndSok;
	private DatagramPacket rcvPkt1, rcvPkt2, sndPkt;
	private InetAddress target1, target2;

	public Host() {
		try {
			port23 = new DatagramSocket(23);
			sndRcvSok = new DatagramSocket();
			sndSok = new DatagramSocket();
			target1 = InetAddress.getLocalHost();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		verbose = false;
		reset = false;
		test = 0;
		targetPort = 69;

		new UI().start();
	}

	/**
	 * Receives data from the given socket into the given packet.
	 * @param pkt
	 * @param sock
	 */
	private void receive(DatagramPacket pkt, DatagramSocket sock) {
		try {
			sock.receive(pkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Changes the opcode of the received packet to 15
	 * @param pkt
	 */
	private byte[] changeOpcode(DatagramPacket pkt) {
		byte[] data = pkt.getData();
		data[0] = errorReq == CHANGEOPCODE ? (byte)1 : (byte)0;
		data[1] = errorReq == CHANGEOPCODE ? (byte)5 : (byte)4;	
		return data;
	}

	/**
	 * Changes the length of the packet to 530 bytes
	 */
	private byte[] changeLength(DatagramPacket pkt) {
		int i = 0;
		byte[] data = new byte[530];


		while (i < pkt.getLength())
		{

			data[i]= pkt.getData()[i++];
		}

		while (i < 530) data[i++] = 0x05;

		return data;
	}

	/**
	 * Sends data from the given packet through the given socket.
	 * @param pkt
	 * @param sock
	 */
	private void send(DatagramPacket pkt, DatagramSocket sock) {
		try {
			sock.send(pkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the sockets and exits.
	 */
	private void quit() {
		if (verbose) System.out.println("Closing port 23");
		port23.close();
		if (verbose) System.out.println("Closing sndSok");
		sndSok.close();
		if (verbose) System.out.println("Closing sndRcvSok");
		sndRcvSok.close();

		System.out.println("Exiting");
		System.exit(0);
	}

	private int getTest() {
		return this.test;
	}
	private void delay(int ms){
		try {
		Thread.sleep(ms);
		} catch (InterruptedException e) {
		e.printStackTrace();
		}

	}
	public boolean isReset() {
		boolean reset = this.reset;
		this.reset = false;

		return reset;
	}

	/**
	 * Receives data from the client.
	 * <p>
	 * Listens on port 23 for a request.
	 */
	public void rcvP23() {
		rcvPkt1 = new DatagramPacket(new byte[receiveLength], receiveLength);

		if (verbose) System.out.print("Listening on ");
		if (verbose) System.out.println(port23.getLocalPort());

		receive(rcvPkt1, port23);

		if (errorReq == CHANGEOPCODE || errorReq == CHANGEOPCODEv) {
			byte[] data = changeOpcode(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = NORMAL;
		} else if (errorReq == CHANGELENGTH) {
			byte[] data = changeLength(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = CHANGELENGTH;
		}

		if (verbose) {
			System.out.println("Client ");
			System.out.print("Opcode ");
			System.out.println(new Integer(rcvPkt1.getData()[1]));
			System.out.println(new String(rcvPkt1.getData()));
		}
	}

	/**
	 * Receives data from the client.
	 * <p>
	 * Listens on TID port for data.
	 */
	public void receive1() {
		rcvPkt1 = new DatagramPacket(new byte[receiveLength], receiveLength);

		if (verbose) System.out.print("Listening on ");
		if (verbose) System.out.println(sndSok.getLocalPort());

		receive(rcvPkt1, sndSok);

		if (verbose) {
			System.out.println("Client ");
			System.out.print("Opcode ");
			System.out.println(new Integer(rcvPkt1.getData()[1]));
			System.out.println(new String(rcvPkt1.getData()));
		}
	}

	/**
	 * Receives a reply from the server.
	 * <p>
	 * Listens for a reply on the socket used to forward data to the server.
	 */
	public void receive2() {
		rcvPkt2 = new DatagramPacket(new byte[receiveLength], receiveLength);

		if (verbose) System.out.print("Listening on ");
		if (verbose) System.out.println(sndRcvSok.getLocalPort());

		receive(rcvPkt2, sndRcvSok);
		if (verbose) {
			System.out.println("Server ");
			System.out.print("Opcode ");
			System.out.println(new Integer(rcvPkt2.getData()[1]));
			System.out.println(new String(rcvPkt2.getData()));
		}
	}

	/**
	 * Forwards the request from the client to the server.
	 * <p>
	 * Should not be called before receive1().
	 */
	public void forward() {
		
		DatagramSocket errorSocket;
		clientPkt--;
		sndPkt = new DatagramPacket(rcvPkt1.getData(), rcvPkt1.getLength(), target1, targetPort);
		// Save the client IP and port to send the server's response.
		target2 = rcvPkt1.getAddress();
		returnPort = rcvPkt1.getPort();

		System.out.println(clientPkt);
		if (errorReq == CHANGETIDCLIENT && clientPkt==0) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				errorSocket.close();
				errorReq = NORMAL;
			} catch (SocketException e) {

			}
		} else { 
			packetNum--;
			if(errorReq == DELAYTOSERVER){
			
				
					
				delay(delay);
				errorReq=NORMAL;
				
			}
			
			if(errorReq==LOSECLIENTPKT &&packetNum == 0){
				errorReq=NORMAL;
			}else{
			send(sndPkt, sndRcvSok);
			}
			if (errorReq == DUPLICATECLIENTPKT&&packetNum ==0)
			{
				delay(delay);
				send(sndPkt, sndRcvSok);
				errorReq = NORMAL;
				
			}
			
		}
	}

	/**
	 * Sends the server's reply to the client.
	 * <p>
	 * Should not be called before receive2().
	 */
	public void forwardReply() {
		DatagramSocket errorSocket;
		serverPkt--;
		sndPkt = new DatagramPacket(rcvPkt2.getData(), rcvPkt2.getLength(), target2, returnPort);

		targetPort = rcvPkt2.getPort();
		System.out.println(serverPkt);
		if (errorReq == CHANGETIDSERVER && serverPkt==0) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				System.out.println("New port: " + errorSocket.getLocalPort());
				errorSocket.close();
				errorReq = NORMAL;
			} catch (SocketException e) {

			}
		} else {
			packetNum--;
			if(errorReq == DELAYTOCLIENT&& packetNum ==0){
				
		
					
				delay(delay);
				
				errorReq=NORMAL;
				
			}
			if(errorReq==LOSESERVERPKT &&packetNum==0){
				errorReq=NORMAL;
			}else{
				send(sndPkt, sndSok);
			}
			if(errorReq == DUPLICATESERVERPKT &&packetNum == 0){
				send(sndPkt, sndSok);
				errorReq = NORMAL;
			}
			
		}
	}

	/**
	 * UI
	 * @author MatthewPenner
	 * The UI class handles user inputs. It allows the user input to be read during transfers.
	 */
	private class UI extends Thread {
		private Boolean quit;

		public UI () {
			quit = false;
		}

		/**
		 * Prints out the user's options.
		 */
		private void printUI() {
			System.out.println("T - Toggle test mode");
			System.out.println("V - Toggle verbose mode");
			System.out.println("E - View error simulator options");
			System.out.println("Q - Quit");
			System.out.println("Restart this between transfers.");
			System.out.print("Test: "); System.out.print(test); System.out.print("    Verbose: "); System.out.println(verbose);
		}

		/**
		 * Prints the options and receives the user's inputs.
		 * @throws IOException
		 */
		public void ui() throws IOException {
			String command;
			Scanner input = new Scanner(System.in);

			while (!quit) {
				printUI();
				command = input.nextLine();

				switch (command.toLowerCase().charAt(0)) {
					case 'q': quit = true;
							  quit();
							  break;
					case 't': getTest();
							  break;
					case 'v': verbose = !verbose;
						  	  break;
					case 'r': reset = true;
						  	  break;
					case 'e': listErrors();
							  break;
					case '1': errorReq = CHANGEOPCODE;
						  	  break;
					case '2': errorReq = CHANGELENGTH;
							  break;
					case '3': errorReq = CHANGETIDSERVER;
								getServerPacket();
							  break;
					case '4': errorReq = CHANGETIDCLIENT;
							getClientPacket();
							  break;
					case '5': errorReq = CHANGEOPCODEv;
							  break;
					case '6': errorReq = DELAYTOSERVER;
							askForParameters();
							break;
					case '7': errorReq = DELAYTOCLIENT;
							askForParameters();
							break;
					case '8': errorReq = DUPLICATECLIENTPKT;
							askForParameters();
							break;
					case '9': errorReq = DUPLICATESERVERPKT;
							askForParameters();
							break;
					case 'a': errorReq = LOSECLIENTPKT;
							getPacket();
							break;
					case 'b': errorReq = LOSESERVERPKT;
							getPacket();
							break;
				}
			}
		}
		public void askForParameters(){
			Scanner input = new Scanner(System.in);
			System.out.println("How much delay between packets(in ms)?");
			delay = input.nextInt();
			System.out.println("Which packet #?");
			packetNum = input.nextInt();
			
		}
		public void getPacket(){
			Scanner input = new Scanner(System.in);
			System.out.println("Which packet #?");
			packetNum = input.nextInt();
		}
		public void getServerPacket(){
			Scanner input = new Scanner(System.in);
			System.out.println("Which packet #?");
			serverPkt = input.nextInt();
		}
		public void getClientPacket(){
			Scanner input = new Scanner(System.in);
			System.out.println("Which packet #?");
			clientPkt = input.nextInt();
		}
		public void listErrors() {
			System.out.println("1 - Change Opcode (invalid opcode)");
			System.out.println("2 - Change Length");
			System.out.println("3 - Change Transfer ID of server");
			System.out.println("4 - Change Transfer ID of client.");
			System.out.println("5 - Change Opcode (valid opcode)");
			System.out.println("6 - Delay transfer to Server");
			System.out.println("7 - Delay transfer to Client");
			System.out.println("8 - Duplicate packet to Server");
			System.out.println("9 - Duplicate packet to Client");
			System.out.println("a - Lose packet to Server");
			System.out.println("b - Lose packet to Client");
		}

		public void run() {
			try {
				this.ui();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		Host host = new Host();
		host.rcvP23();

		while (true) {
			host.forward();
			host.receive2();
			host.forwardReply();
			host.receive1();
		}
	}

}
