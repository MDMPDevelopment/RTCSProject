import java.net.DatagramSocket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private static final int receiveLength = 100;
	private static final byte readReq = 0x01;
	private static final byte writeReq = 0x02;
	private static final String mode = "octet";
	
	private DatagramSocket sock;
	private DatagramPacket sndPkt, rcvPkt;
	private InetAddress target;
	
	private int port;
	
	private boolean test, verbose;
	
	public Client() throws UnknownHostException, SocketException {
		target = InetAddress.getLocalHost();
		
		test = false;
		verbose = false;
		
		sock = new DatagramSocket();
	}
	
	/**
	 * Displays the user options.
	 */
	private void printUI() {
		System.out.println("T - Toggle test mode");
		System.out.println("V - Toggle verbose mode");
		System.out.println("W - Initiate file write");
		System.out.println("R - Initiate file read");
		System.out.println("Q - Quit");
	}
	
	/**
	 * Prompts the user to enter the name of the file to be transferred.
	 * @return The name of the file to be transferred.
	 */
	private String pickFile() {
		String file;
		Scanner stream = new Scanner(System.in);
		System.out.println("Enter filename.");
		file = stream.nextLine();
		stream.close();
		
		return file;
	}
	
	/**
	 * Send sndPkt from sock.
	 * <p>
	 * Note that this function should never be called until after createPkt has been called.
	 */
	private void send() {
		try {
			sock.send(sndPkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Receive from sock into rcvPkt.
	 */
	private void receive() {
		rcvPkt = new DatagramPacket(new byte[516], receiveLength);
		
		try {
			sock.receive(rcvPkt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void quit() {
		sock.close();
		System.exit(0);
	}
	
	private void startWrite() throws IOException {
		int sizeRead;
		byte[] block = {0x00, 0x01};
		byte[] opcode = {0x00, 0x03};
		String file = pickFile();
		byte[] request = buildRQ(file, writeReq);
		byte[] data = new byte[512];
		BufferedInputStream in = new BufferedInputStream(new FileInputStream("Client/" + file));
		
		sndPkt = new DatagramPacket(request, request.length, target, 69);
		send();
		receive();
		port = rcvPkt.getPort();
		target = rcvPkt.getAddress();
		
		sizeRead = in.read(data);
		
		while (sizeRead != -1) {
			request = new byte[4 + sizeRead];
			System.arraycopy(opcode, 0, request, 0, 2);
			System.arraycopy(block, 0, request, 2, 2);
			System.arraycopy(data, 0, request, 4, sizeRead);
			sndPkt = new DatagramPacket(request, request.length, target, port);
			System.out.println(sndPkt.getLength());
			send();
			receive();
			
			if (++block[1] == 0) block[0]++;
			sizeRead = in.read(data);
		}
		in.close();
	}
	
	private void startRead() throws IOException {
		byte[] data;
		String file = pickFile();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("Client/" + file));

		byte[] request = buildRQ(file, readReq);
		byte[] block = {0x00, 0x01};
		byte[] opcode = {0x00, 0x04};
		
		sndPkt = new DatagramPacket(request, request.length, target, 69);
		send();
		receive();
		port = rcvPkt.getPort();
		
		do {
			data = new byte[rcvPkt.getLength() - 4];
			System.arraycopy(rcvPkt.getData(), 4, data, 0, rcvPkt.getLength() - 4);
			out.write(data, 0, data.length);
			
			request = new byte[4];
			System.arraycopy(opcode, 0, request, 0, 2);
			System.arraycopy(block, 0, request, 2, 2);
			
			sndPkt = new DatagramPacket(request, request.length, target, port);
			send();
			receive();
			
			if (++block[1] == 0) block[0]++;
		} while (rcvPkt.getData().length > 511);
		out.close();
	}
	
	private byte[] buildRQ(String file, byte opcode) {
		byte[] request;
		byte[] code = {0x00, opcode};
		request = new byte[file.length() + mode.length() + 4];
		
		System.arraycopy(code, 0, request, 0, 2);		
		System.arraycopy(file.getBytes(), 0, request, 2, file.length());
		request[file.length() + 2] = 0x00;
		//System.out.println(new String(request));
		System.arraycopy(mode.getBytes(), 0, request, file.length() + 3, mode.length());
		request[request.length - 1] = 0x00;
		
		return request;
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	public void ui() throws IOException {
		String command;
		Scanner input = new Scanner(System.in);
		
		while (true) {
			printUI();
			command = input.nextLine();
			
			switch (command.toLowerCase().charAt(0)) {
				case 'q': input.close();
						  quit();
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
	
	public static void main(String[] args) throws IOException {
		Client client = new Client();
		client.ui();
	}
}
