
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
	private static final int CHANGEOPCODECLIENT = 1;
	private static final int CHANGEOPCODESERVER = 2;
	private static final int CHANGELENGTHCLIENT = 3;
	private static final int CHANGELENGTHSERVER = 4;
	private static final int CHANGETIDSERVER = 5;
	private static final int CHANGETIDCLIENT = 6;
	private static final int CHANGEOPCODECLIENTv = 7;
	private static final int CHANGEOPCODESERVERv = 8;
	private static final int DELAYCLIENT = 9;
	private static final int DELAYSERVER = 10;
	private static final int DUPLICATESERVERPKT = 11;
	private static final int DUPLICATECLIENTPKT = 12;
	private static final int LOSESERVERPKT = 13;
	private static final int LOSECLIENTPKT = 14;
	private static final int CHANGEMODE = 15;
	private static final String invalidMode = "sdkgjadfga";
	private boolean  verbose, reset;
	private int targetPort, returnPort, errorReq,delay, packetNum, serverPkt, clientPkt;
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
		data[0] = (byte)0;
		if(errorReq == CHANGEOPCODECLIENT || errorReq==CHANGEOPCODESERVER)
		{
			data[1]=(byte)9;
		}else{
			data[1] = (byte)4;
		}
		return data;
	}
	private void changeMode(){
		byte[] data = rcvPkt1.getData();
		byte[] newmode = invalidMode.getBytes();
		int i = 2;
		while (data[i] != 0x00 && i < rcvPkt1.getLength()) {
			i++;
		}
		i++;
		int j =0;
	
		while (data[i] != 0x00 && i < rcvPkt1.getLength() && j < "netascii".length()) {
			data[i++] = newmode[j++]; 
			
			
		}
		rcvPkt1.setData(data);
	
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

		if (errorReq==CHANGEMODE){
		changeMode();
		errorReq=NORMAL;
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
			System.out.print("Block ");
			System.out.println(0xff & rcvPkt1.getData()[3] + 256 * (0xff & rcvPkt1.getData()[2]));
			System.out.println();
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
			System.out.print("Block ");
			System.out.println(0xff & rcvPkt2.getData()[3] + 256 * (0xff & rcvPkt2.getData()[2]));
			System.out.println();
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
		if ((errorReq == CHANGEOPCODECLIENT || errorReq == CHANGEOPCODECLIENTv) && clientPkt ==0) {
			byte[] data = changeOpcode(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = NORMAL;
		} else if (errorReq == CHANGELENGTHCLIENT &&clientPkt ==0) {
			byte[] data = changeLength(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = NORMAL;
		}
		sndPkt = new DatagramPacket(rcvPkt1.getData(), rcvPkt1.getLength(), target1, targetPort);
		// Save the client IP and port to send the server's response.
		target2 = rcvPkt1.getAddress();
		returnPort = rcvPkt1.getPort();


		if (errorReq == CHANGETIDCLIENT && clientPkt==0) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				errorSocket.close();
				errorReq = NORMAL;
				if (sndPkt.getData()[1] == (byte)0x03) {
					receive1();
					send(sndPkt, sndRcvSok);
				}
				else if (sndPkt.getData()[1] == (byte) 0x04) return;
			} catch (SocketException e) {

			}
		} else { 
			if(errorReq == DELAYCLIENT || errorReq == DUPLICATECLIENTPKT ||errorReq == LOSECLIENTPKT) {
				packetNum--;
			}
			if(errorReq == DELAYCLIENT&&packetNum ==0) {
				delay(delay);
				errorReq=NORMAL;
			}

			if(errorReq==LOSECLIENTPKT &&packetNum == 0){
				errorReq=NORMAL;
				if (rcvPkt1.getData()[1] <= (byte)0x03) receive1();
				else if (rcvPkt1.getData()[1] == 0x04) return;
			}

			send(sndPkt, sndRcvSok);

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

		if ((errorReq == CHANGEOPCODESERVER || errorReq == CHANGEOPCODESERVERv) && serverPkt ==0) {
			byte[] data = changeOpcode(rcvPkt2);
			rcvPkt2.setData(data);
			errorReq = NORMAL;
		} else if (errorReq == CHANGELENGTHSERVER &&serverPkt==0) {
			byte[] data = changeLength(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = NORMAL;
		}

		sndPkt = new DatagramPacket(rcvPkt2.getData(), rcvPkt2.getLength(), target2, returnPort);

		targetPort = rcvPkt2.getPort();
		if (errorReq == CHANGETIDSERVER && serverPkt==0) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				System.out.println("New port: " + errorSocket.getLocalPort());
				errorSocket.close();
				errorReq = NORMAL;
				if (sndPkt.getData()[1] == (byte)0x03) receive2();
				else if (sndPkt.getData()[1] == (byte) 0x04) return;
			} catch (SocketException e) {

			}
		} else {
			if(errorReq == DELAYSERVER || errorReq == DUPLICATESERVERPKT ||errorReq == LOSESERVERPKT)
			{
				packetNum--;
			}
			if(errorReq == DELAYSERVER&& packetNum ==0){
				delay(delay);
				errorReq=NORMAL;
			}
			if(errorReq==LOSESERVERPKT &&packetNum==0){
				errorReq=NORMAL;
				if (rcvPkt2.getData()[1] == (byte)0x03) receive2();
				else if (rcvPkt2.getData()[1] == (byte)0x04) return;
			}

			send(sndPkt, sndSok);

			if(errorReq == DUPLICATESERVERPKT &&packetNum == 0){
				delay(delay);
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
			System.out.println("V - Toggle verbose mode");
			System.out.println("E - View error simulator options");
			System.out.println("Q - Quit");
			System.out.println("Restart this between transfers.");
			System.out.print("Verbose: "); System.out.println(verbose);
		}

		/**
		 * Prints the options and receives the user's inputs.
		 * @throws IOException
		 */
		public void ui() throws IOException {
			String command;
			int x;
			Scanner input = new Scanner(System.in);

			while (!quit) {
				printUI();
				command = input.nextLine();

				switch (command.toLowerCase().charAt(0)) {
				case 'q': quit = true;
				quit();
				break;
				case 'v': verbose = !verbose;
				break;
				case 'r': reset = true;
				break;
				case 'e': listErrors();
				break;
				case '1': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = CHANGEOPCODECLIENT;
					getClientPacket();
				}else if(x == 2){
					errorReq= CHANGEOPCODESERVER;
					getServerPacket();
				}
				break;
				case '2': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = CHANGELENGTHCLIENT;
					getClientPacket();
				}else if(x == 2){
					errorReq= CHANGELENGTHSERVER;
					getServerPacket();
				}
				break;
				case '3': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = CHANGETIDCLIENT;
					getClientPacket();
				}else if(x == 2){
					errorReq= CHANGETIDSERVER;
					getServerPacket();
				}
				break;
				case '4': errorReq=CHANGEMODE;
				break;
				case '5': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = CHANGEOPCODECLIENTv;
					getClientPacket();
				}else if(x == 2){
					errorReq= CHANGEOPCODESERVERv;
					getServerPacket();
				}
				break;
				case '6': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = DELAYCLIENT;
				}else if(x == 2){
					errorReq= DELAYSERVER;
				}
				askForParameters();
				break;

				case '7': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = DUPLICATECLIENTPKT;
				}else if(x == 2){
					errorReq= DUPLICATESERVERPKT;
				}
				askForParameters();
				break;

				case '8': x = promptClientOrServer(); 
				if(x == 1){
					errorReq = LOSECLIENTPKT;
				}else if(x == 2){
					errorReq= LOSESERVERPKT;
				}
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
		public int promptClientOrServer(){
			Scanner input = new Scanner(System.in);
			System.out.println("Client (1) or Server (2)?");
			return input.nextInt();
		}
		public void listErrors() {
			System.out.println("1 - Change Opcode (invalid opcode)");
			System.out.println("2 - Change Length");
			System.out.println("3 - Change Transfer ID");
			System.out.println("4 - Change mode (invalid)");
			System.out.println("5 - Change Opcode (valid opcode)");
			System.out.println("6 - Delay transfer");
			System.out.println("7 - Duplicate packet");
			System.out.println("8 - Lose packet");
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
