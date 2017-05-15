import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Scanner;

public class Server {
	private static final String netascii = "netascii";
	private static final String octet = "octet   ";
	
	private DatagramSocket port69;
	private byte[] mode, file;
	
	private DatagramPacket request;
	
	private Boolean valid, test, verbose;
	
	public Server() {
		try {
			port69 = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		verbose = false;
		test = false;
		
		new UI().start();
	}
	
	/**
	 * Closes the port and exits. Outstanding transfers will run to completion.
	 */
	private void quit() {
		port69.close();
		System.exit(0);
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
	 * 
	 **/
	public byte[] createErrorMsg(byte type, byte[] errorMsg)
	{
		byte msg[] = new byte[errorMsg.length + 5];
		
		msg[0] = (byte)0;
		msg[1] = (byte)5;
		msg[2] = (byte)0;
		msg[3] = type;
		
		System.arraycopy(errorMsg, 0, msg, 4, errorMsg.length);
		
		msg[msg.length - 1] = (byte)0;
				
		return msg;
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
		Transfer transfer;
		
		if (verbose) System.out.println("Parsing packet.");
		
		byte[] data = request.getData();
		
		if (verbose) {
			System.out.print("Opcode ");
			System.out.println(new Integer(data[1]));
		}
		
		file = new byte[data.length];
		mode = new byte[netascii.length()];
		int i = 2;
		int j = 0;
		
		valid = data[0] == 0x00;
		valid = (data[1] == 0x01 || data[1] == 0x02) && valid;
		
		// Read out the filename from the request.
		while (data[i] != 0x00 && i < data.length) {
			file[j++] = data[i++];
			if (verbose) System.out.print((char)file[j - 1]);
		}
		
		if (verbose) System.out.println();
		
		// Valid if the filename is one or more characters long and there is data after the terminating 0x00.
		valid = i > 2 && i++ < data.length && valid;
		
		j = 0;
		
		// Read out the mode from the request.
		while (data[i] != 0x00 && i < data.length && j < netascii.length()) {
			mode[j++] = data[i++]; 
			if (verbose) System.out.print((char)mode[j - 1]);
		}
		
		if (verbose) System.out.println();
		
		//valid = (new String(mode).substring(0, netascii.length()).toLowerCase().equals(netascii) || new String(mode).substring(0, octet.length()).toLowerCase().equals(octet)) && valid;
		valid = data[i] == 0x00 && valid;
		
		// If the packet is a valid request, start a new transfer.
		if (valid) {
			if (verbose) System.out.println("Valid request.  Starting transfer.");
			transfer = new Transfer(data[1] == 0x02, request, new String(file));
			transfer.start();
		} else {
			byte [] emsg = "Error 4: Illegal TFTP operation".getBytes();
			System.out.println("Error 4: Illegal TFTP operation");
			DatagramSocket sock;
			try {
				sock = new DatagramSocket();
				sock.send(new DatagramPacket(createErrorMsg((byte)4, emsg), 5 + emsg.length, request.getAddress(), request.getPort()));	//send error
				sock.close();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		
	
			return;
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
			System.out.println("Q - Quit");
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
		private byte[] rData = new byte[516];
		
		/**
		 * Constructor for the Transfer class
		 * @param type If true, the transfer is a write request.  Else, it's a read request.
		 * @param request The request which spawned the transfer.
		 * @param filename The name of the file requested.
		 * @param verbose Indicates whether this transfer is in verbose mode.
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
		
		/**
		 * Sends the packet passed to the connected client.
		 * @param pkt
		 */
		private void send(DatagramPacket pkt) {
			try {
				sock.send(pkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Waits to receive a packet from the connected client.
		 */
		public void receive() {
			rPkt = new DatagramPacket(rData, 516);
			try {
				sock.receive(rPkt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Services a write transfer.  Reads from the client over the network and writes the file locally.
		 * @throws IOException
		 */
		private void write() throws IOException {
			byte[] data;
			
			// Opens the file to write.
			if (verbose) System.out.println("Opening file.");
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
			
			byte[] response = new byte[4];
			byte[] block = {0x00, 0x00};
			
			// Build and send the request response.
			response[0] = 0x00;
			response[1] = 0x04;
			System.arraycopy(block, 0, response, 2, 2);
			block[1]++;
			
			if (verbose) System.out.println(new String(response));
			if (verbose) System.out.println("Sending response.");
			sPkt = new DatagramPacket(response, response.length, target, port);
			send(sPkt);
			
			if (verbose) System.out.println("Starting write.");

			/*
			 * While the packet length is 516 bytes (4 byte header, 512 bytes data):
			 *    - Receive the next data packet.
			 *    - Increment the block number.
			 *    - Separate the data from the header.
			 *    - Write the data to the file.
			 *    - Build and send the response.
			 */
			do {
				receive();
				
				if (verbose) System.out.print("Received ");
				if (verbose) System.out.println(new String(rPkt.getData()));
				if (verbose) System.out.print(rPkt.getLength());
				if (verbose) System.out.println(" bytes");
				
				if (++block[1] == 0) block[0]++;
				
				data = new byte[rPkt.getLength() - 4];
				System.arraycopy(rPkt.getData(), 4, data, 0, rPkt.getLength() - 4);
				
				if (verbose) System.out.print("Output ");
				if (verbose) System.out.println(new String(data));
				out.write(data, 0, data.length);
				out.flush();
				
				response = new byte[4];
				response[0] = 0x00;
				response[1] = 0x04;
				System.arraycopy(block, 0, response, 2, 2);
				
				sPkt = new DatagramPacket(response, response.length, target, port);
				send(sPkt);
			} while (rPkt.getLength() > 515);
			out.close();
			if (verbose) System.out.println("Finished write.");
		}
		
		/**
		 * Services a read transfer.  Reads from local file and writes to the client over the network.
		 * @throws IOException
		 */
		private void read() throws IOException {
			int sizeRead;
			byte[] response;
			byte[] block = {0x00, 0x00};
			byte[] data = new byte[512];
			
			if (verbose) System.out.println("Opening file.");
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
			
			sizeRead = in.read(data);
			
			if (verbose) System.out.println("Starting read.");
			while (sizeRead != -1) {
				if (++block[1] == 0) block[0]++;
				
				if (verbose) System.out.print("Read ");
				if (verbose) System.out.println(new String(data));
				
				response = new byte[sizeRead + 4];
				
				response[0] = 0x00;
				response[1] = 0x03;
				System.arraycopy(block, 0, response, 2, 2);
				System.arraycopy(data, 0, response, 4, sizeRead);
				
				if (verbose) System.out.print("Sent ");
				if (verbose) System.out.println(new String(response));
				
				sPkt = new DatagramPacket(response, sizeRead + 4, target, port);
				send(sPkt);
				receive();
				
				sizeRead = in.read(data);
			}
			in.close();
			if (verbose) System.out.println("Finished read.");
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
		
		while (true) {
			server.receive();
			server.parsePacket();
		}
	}
}
