import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private static final int sendLength = 100;
	private static final int receiveLength = 100;
	private static final int sendPort = 23;
	private static final byte readReq = 0x01;
	private static final byte writeReq = 0x02;
	private static final String mode = "octet";
	
	private DatagramSocket sock;
	private DatagramPacket sndPkt, rcvPkt;
	private InetAddress target;
	
	private boolean test, verbose;
	
	private byte[] sndData, rcvData;
	
	public Client() throws UnknownHostException, SocketException {
		target = InetAddress.getLocalHost();
		sndData = new byte[sendLength];
		rcvData = new byte[receiveLength];
		
		test = false;
		verbose = false;
		
		sock = new DatagramSocket();
	}
	
	private void printUI() {
		System.out.println("T - Toggle test mode");
		System.out.println("V - Toggle verbose mode");
		System.out.println("W - Initiate file write");
		System.out.println("R - Initiate file read");
		System.out.println("Q - Quit");
	}
	
	private String pickFile() {
		System.out.println("Enter filename.");
		return new Scanner(System.in).nextLine();
	}
	
	/**
	 * Build DatagramPacket to send.
	 * <p>
	 * Builds a packet with a data array of the following format:
	 * [0x00, packetType, fileName, 0x00, mode, 0x00]
	 * packetType is 0x01 if the packet is for a read request, 0x02 if for a write request, and anything else if for an invalid request.
	 *  
	 * @param pktType The type of packet to be created.  0 if read, 1 if write, 2 if invalid.
	 */
	public void createPkt(int pktType, String path) {
		int i = 0;
		byte[] fileName = path.getBytes();
		byte[] modeByte = mode.getBytes();
		
		// Set the first two bytes of the data buffer according to the request type.
		sndData[0] = 0x00;
		sndData[1] = pktType == 1 ? writeReq : readReq;
		if (pktType == 2) sndData[1] = 0x05;
		
		// Copy over the file name to the data buffer.
		while (i < fileName.length) {
			sndData[2 + i] = fileName[i++];
		}
		
		i++;
		
		// Add the separating zero byte.
		sndData[i++] = 0x00;
		
		// Copy over the mode to the data buffer.
		for (int j=0; j < modeByte.length; j++) {
			sndData[i++] = modeByte[j];
		}
		
		// Add the terminating zero byte.
		sndData[i] = 0x00;
		
		// Print the data to be sent as a String.
		System.out.println(new String(sndData));
		
		// Print the data to be sent as bytes.
		try {
			System.out.write(sndData);
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		sndPkt = new DatagramPacket(sndData, i, target, sendPort);
	}
	
	/**
	 * Send sndPkt from sock.
	 * <p>
	 * Note that this function should never be called until after createPkt has been called.
	 */
	public void send() {
		try {
			sock.send(sndPkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Receive from sock into rcvPkt.
	 */
	public void receive() {
		rcvPkt = new DatagramPacket(rcvData, receiveLength);
		
		try {
			sock.receive(rcvPkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Prints out the data contained in rcvPkt.
	 * <p>
	 * Note that this function should never be called until after receive has been called.
	 */
	public void printData() {
		System.out.println(new String(rcvPkt.getData()));
	}
	
	public void ui() {
		String command;
		Scanner input = new Scanner(System.in);
		
		while (true) {
			printUI();
			command = input.nextLine();
			
			switch (command.toLowerCase().charAt(0)) {
				case 'q': quit();
						  break;
				case 't': test = !test;
						  break;
				case 'v': verbose = !verbose;
						  break;
				case 'w': startWrite();
						  break;
				case 'r': startRead();
						  break;
			}
		}
	}
	
	public void quit() {
		System.exit(1);
	}
	
	public void startWrite() {
		int sizeRead;
		String file = pickFile();
		byte[] data = new byte[512];
		BufferedInputStream in = new BufferedInputStream(new FileInputStream("file"));
		
		while (sizeRead = in.read(data) != -1) {
			createPkt(data, sizeRead);
			send();
			receive();
			parseResponse();
		}
	}
	
	public void startRead() {
		
	}
	
	public static void main(String[] args) throws UnknownHostException, SocketException {
		Client client = new Client();
		client.ui();
		
		/*
		for (int i=0; i < 11; i++) {
			client.createPkt(i != 10 ? i % 2 : 2);
			client.send();
			client.receive();
			client.printData();
		}*/
	}
}
