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
	
	private DatagramSocket port23, sndRcvSok, sndSok;
	private DatagramPacket rcvPkt1, rcvPkt2, sndPkt;
	private InetAddress target1, target2;
	private int targetPort;
	private int returnPort;
	private int test;
	private Boolean verbose;
	private Boolean transfer;
	private Boolean reset;
	private int errorReq;
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
		transfer = false;
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
	private byte[] changeOpcode(DatagramPacket pkt)
	{
		byte[] data = pkt.getData();
		data[0] = 1;
			data[1] = 5;	
		return data;
		
	}
	/**
	 * Changes the length of the packet to 530 bytes
	 */
	private byte[] changeLength(DatagramPacket pkt)
	{
		byte[] data = new byte[530];
		for (int i = 0; i<pkt.getLength(); i++)
		{
			data[i]= pkt.getData()[i];
		}
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
	 * Prints out the data buffer from pkt as bytes and as a String.
	 * @param pkt
	 */
	private void printData(DatagramPacket pkt) {
		try {
			System.out.write(pkt.getData());
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println(new String(pkt.getData()));
	}
	
	/**
	 * Closes the sockets and exits.
	 */
	private void quit() {
		port23.close();
		sndSok.close();
		sndRcvSok.close();
		System.exit(0);
	}
	
	private void getTest() {
		
	}
	
	public Boolean isReset() {
		Boolean reset = this.reset;
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
		if (errorReq ==CHANGEOPCODE)
		{
			byte [] data = changeOpcode(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq=NORMAL;
		}else if(errorReq == CHANGELENGTH)
		{
			byte [] data = changeLength(rcvPkt1);
			rcvPkt1.setData(data);
			errorReq = CHANGELENGTH;
		}
		if (verbose) System.out.print("Client ");
		if (verbose) System.out.println(new String(rcvPkt1.getData()));
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

		if (verbose) System.out.print("Client ");
		if (verbose) System.out.println(new String(rcvPkt1.getData()));
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
		if (verbose) System.out.print("Server ");
		if (verbose) System.out.println(new String(rcvPkt2.getData()));
	}
	
	/**
	 * Forwards the request from the client to the server.
	 * <p>
	 * Should not be called before receive1().
	 */
	public void forward() {
		//printData(rcvPkt1);
		DatagramSocket errorSocket;
		
		sndPkt = new DatagramPacket(rcvPkt1.getData(), rcvPkt1.getLength(), target1, targetPort);
		// Save the client IP and port to send the server's response.
		target2 = rcvPkt1.getAddress();
		returnPort = rcvPkt1.getPort();
		
		//printData(sndPkt);
		
		if (errorReq == CHANGETIDCLIENT) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				errorSocket.close();
			} catch (SocketException e) {
				
			}
		}
		else send(sndPkt, sndRcvSok);
	}
	
	/**
	 * Sends the server's reply to the client.
	 * <p>
	 * Should not be called before receive2().
	 */
	public void forwardReply() {
		DatagramSocket errorSocket;
		
		sndPkt = new DatagramPacket(rcvPkt2.getData(), rcvPkt2.getLength(), target2, returnPort);
		
		targetPort = rcvPkt2.getPort();
		
		if (errorReq == CHANGETIDSERVER) {
			try {
				errorSocket = new DatagramSocket();
				send(sndPkt, errorSocket);
				errorSocket.close();
			} catch (SocketException e) {
				
			}
		} else send(sndPkt, sndSok);
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
					case 'e' : listErrors();
							  break;
					case '1' :	 errorReq = CHANGEOPCODE;
							  break;
					case '2' : errorReq = CHANGELENGTH;
							  break;
					case '3': errorReq = CHANGETIDSERVER;
							  break;
					case '4': errorReq = CHANGETIDCLIENT;
							  break;
				}
			}
		}
		public void listErrors(){
			System.out.println("1 - Change Opcode");
			System.out.println("2 - Change length");
			System.out.println("3 - Change Transfer ID of server");
			System.out.println("4 - Change Transfer ID of client.");
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
