import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
	private static final String netascii = "netascii";
	private static final String octet = "octet   ";
	
	private DatagramSocket port69;
	private byte[] mode, file;
	
	private DatagramPacket request;
	
	private Boolean valid, test, verbose;
	//private ArrayList<Transfer> transfers;
	
	public Server() {
		try {
			port69 = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		verbose = false;
		test = false;
		
		new UI().start();
		
		//transfers = new ArrayList<Transfer>();
	}
	
	private void printUI() {
		System.out.println("T - Toggle test mode");
		System.out.println("V - Toggle verbose mode");
		System.out.println("S - Start server");
		System.out.println("Q - Quit");
	}
	
	private void quit() {
		port69.close();
		System.exit(0);
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
	 * Receives requests.
	 * <p>
	 * Listens on port 69 for requests.
	 */
	public void receive() {
		request = new DatagramPacket(new byte[516], 516);
		try {
			port69.receive(request);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Parses received packets to ensure that they are valid.
	 * <p>
	 * Should not be called before receive().
	 * <p>
	 * Ensures that received packets are of the format:
	 * [0x00, packetType, fileName, 0x00, mode, 0x00]
	 * where packetType is either 0x01 or 0x02 and that mode is either netascii or octet
	 * in any case combination.
	 */
	public void parsePacket() {
		System.out.println("Parsing");
		Transfer transfer;
		byte[] data = request.getData();
		//System.out.println(new String(data));
		file = new byte[data.length];
		mode = new byte[netascii.length()];
		int i = 2;
		int j = 0;
		//System.out.println(valid);
		valid = data[0] == 0x00;
		valid = (data[1] == 0x01 || data[1] == 0x02) && valid;
		
		while (data[i] != 0x00 && i < data.length) {
			file[j++] = data[i++];
		}
		//System.out.println(valid);
		valid = i > 2 && i++ < data.length && valid;
		
		j = 0;
		
		while (data[i] != 0x00 && i < data.length && j < netascii.length()) {
			mode[j++] = data[i++]; 
		}
		//System.out.println(valid);
		//System.out.println(new String(mode).toLowerCase());
		//valid = (new String(mode).toLowerCase().equals(netascii) || new String(mode).toLowerCase().equals(octet)) && valid;
		//System.out.println(valid);
		valid = data[i] == 0x00 && valid;
		//System.out.println(valid);
		if (valid) {
			//System.out.println("Parsed");
			//transfers.add(new Transfer(data[1] == 0x02, request, new String(file)));
			transfer = new Transfer(data[1] == 0x02, request, new String(file));
			transfer.start();
		}
		//System.out.println(valid);
	}
	
	/**
	 * Builds the response packet.
	 * <p>
	 * Should not be called before parsePacket().
	 * <p>
	 * Builds a response with a data buffer {0x00, 0x03, 0x00, 0x01} if the request was a read request.
	 * Builds a response with a data buffer {0x00, 0x04, 0x00, 0x00} if the request was a write request.
	 * @throws IOException
	 */
	/*public void buildResponse() throws IOException {
		printData(rPkt);
		
		byte[] response;
		byte[] data = rPkt.getData();
		
		if (!valid) {
			throw new IOException();
		}
		
		switch (data[1]) {
			case 0x01: response = new byte[] {0x00, 0x03, 0x00, 0x01};
					   break;
			case 0x02: response = new byte[] {0x00, 0x04, 0x00, 0x00};
					   break;
			default: throw new IOException();
		}
		
		sPkt = new DatagramPacket(response, response.length, rPkt.getAddress(), rPkt.getPort());
	}*/
	
	/**
	 * Sends the response.
	 * <p>
	 * Should not be called before buildResponse().
	 */
	/*public void send() {
		try {
			sndSok = new DatagramSocket();
			sndSok.send(sPkt);
			sndSok.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
	
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
				case 's': return;
			}
		}
	}
	
	private class UI extends Thread {
		private Boolean quit;
		
		public UI () {
			quit = false;
		}
		
		private void printUI() {
			System.out.println("T - Toggle test mode");
			System.out.println("V - Toggle verbose mode");
			System.out.println("Q - Quit");
		}
		
		public void ui() throws IOException {
			String command;
			Scanner input = new Scanner(System.in);
			
			while (!quit) {
				printUI();
				command = input.nextLine();
				
				switch (command.toLowerCase().charAt(0)) {
					case 'q': quit = true;
							  input.close();
							  quit();
							  break;
					case 't': test = !test;
							  break;
					case 'v': verbose = !verbose;
							  break;
				}
			}
		}
		
		public void run() {
			try {
				this.ui();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Transfer
	 * @author Matthew
	 * The transfer class handles multi-threaded file transfers.  When the server receives a valid read or write request,
	 * it creates a Transfer to service the request.
	 */
	private class Transfer extends Thread {
		private Boolean type;
		private DatagramSocket sock;
		private InetAddress target;
		private int port;
		private String filename;
		private DatagramPacket sPkt, rPkt;
		private byte[] rData = new byte[216];
		
		/**
		 * Constructor for the Transfer class
		 * @param type If true, the transfer is a write request.  Else, it's a read request.
		 * @param request The request which spawned the transfer.
		 * @param filename The name of the file requested.
		 */
		public Transfer(Boolean type, DatagramPacket request, String filename) {
			this.type = type;
			this.filename = "Server/" + filename.trim();
			
			target = request.getAddress();
			port = request.getPort();
			
			try {
				sock = new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		
		private void send(DatagramPacket pkt) {
			try {
				sock.send(pkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void receive() {
			rPkt = new DatagramPacket(rData, 216);
			try {
				sock.receive(rPkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void write() throws IOException {
			byte[] data;
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
			byte[] response = new byte[4];
			byte[] block = {0x00, 0x00};
			
			response[0] = 0x00;
			response[1] = 0x04;
			System.arraycopy(block, 0, response, 2, 2);
			block[1]++;
			
			sPkt = new DatagramPacket(response, response.length, target, port);
			send(sPkt);
			System.out.println("Here");

			do {
				receive();
				System.out.println(new String(rPkt.getData()));
				if (++block[1] == 0) block[0]++;
				data = new byte[rPkt.getLength() - 4];
				System.arraycopy(rPkt.getData(), 4, data, 0, rPkt.getLength() - 4);
				out.write(data, 0, data.length);
				
				response = new byte[4];
				response[0] = 0x00;
				response[1] = 0x04;
				System.arraycopy(block, 0, response, 2, 2);
				
				sPkt = new DatagramPacket(response, response.length, target, port);
				send(sPkt);
			} while (rPkt.getData().length > 511);
			out.close();
		}
		
		private void read() throws IOException {
			int sizeRead;
			byte[] response;
			byte[] block = {0x00, 0x01};
			byte[] opcode = {0x00, 0x03};
			byte[] data = new byte[512];
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
			
			sizeRead = in.read(data);
			
			while (sizeRead != -1) {
				receive();
				if (++block[1] == 0) block[0]++;
				sizeRead = in.read(data);
				response = new byte[516];
				System.arraycopy(opcode, 0, response, 0, 2);
				System.arraycopy(block, 0, response, 2, 2);
				System.arraycopy(data, 0, response, 4, data.length);
				sPkt = new DatagramPacket(response, sizeRead + 4, target, port);
				send(sPkt);
			}
			in.close();
		}
		
		/**
		 * Starts the file transfer according to what type of request it was.
		 */
		public void run() {
			if (type) {
				try {
					write();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					read();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Server server = new Server();
		//server.ui();
		
		while (true) {
			server.receive();
			server.parsePacket();
		}
		/*while (true) {
			server.receive();
			server.parsePacket();
			try {
				server.buildResponse();
			} catch (IOException e) {
				System.exit(-1);
			}
			server.send();
		}*/
	}
}
