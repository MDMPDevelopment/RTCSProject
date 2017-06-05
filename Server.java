
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Server {
	private static final String netascii = "netascii";
	private static final String octet = "octet";
	private static final String error4 = "Error 4: Illegal TFTP operation";
	private static final String badTID = "Invalid TID";
	private static final int timeout_ms = 500;
	private static final String defaultDir = "./Server/";
	
	private DatagramSocket port69;
	private byte[] mode, file;
	
	private DatagramPacket request;
	
	private Boolean valid, verbose;
	
	private String dir;
	
	public boolean exit;
	
	public Server() {
		try {
			port69 = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		verbose = false;
		exit = false;
		
		dir = defaultDir;
		
		new UI().start();
	}
	
	/**
	 * Closes the port and exits. Outstanding transfers will run to completion.
	 */
	private void quit() {
		if (verbose) System.out.println("Closing port 69.");
		port69.close();
		System.out.println("Exiting.");
		exit = true;
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
	
	private void changeDir() {
		Scanner stream = new Scanner(System.in);
		System.out.println("Enter the full path of the new directory.");
		System.out.println("Please use / instead of \\, and include a terminating /");
		dir = stream.nextLine();
		if (dir.charAt(dir.length() - 1) != '/') dir = dir + "/";
	}
	
	/**
	 * Generates an error message.
	 * <p>
	 * Builds an error message of the format:
	 * {0x00, 0x05, 0x00, type, errorMsg}
	 * @param type Which error condition is present.
	 * @param errorMsg A more detailed message describing the error. 
	 * @return A byte array containing the error.
	 */
	public byte[] createErrorMsg(byte type, byte[] errorMsg) {
		byte msg[] = new byte[errorMsg.length + 5];
		
		msg[0] = 0x00;
		msg[1] = 0x05;
		msg[2] = 0x00;
		msg[3] = type;
		
		System.arraycopy(errorMsg, 0, msg, 4, errorMsg.length);
		
		msg[msg.length - 1] = 0x00;
				
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
		int i, j;
		
		if (verbose) System.out.println("Parsing packet.");
		
		byte[] data = request.getData();
		
		if (verbose) {
			System.out.print("Opcode ");
			System.out.println(new Integer(data[1]));
			System.out.println();
		}
		
		// Valid if the packet is no longer than 516 bytes (4 byte header, 512 bytes of data).
		valid = data.length <= 516;
		
		file = new byte[data.length];
		mode = new byte[netascii.length()];
		
		valid = data[0] == 0x00 && valid;
		valid = (data[1] == 0x01 || data[1] == 0x02) && valid;
		
		// Read out the filename from the request.
		i = 2; j = 0;
		while (data[i] != 0x00 && i < request.getLength()) {
			file[j++] = data[i++];
			if (verbose) System.out.print((char)file[j - 1]);
		}
		
		if (verbose) System.out.println();
		
		// Valid if the filename is one or more characters long and there is data after the terminating 0x00.
		valid = i > 2 && i++ < data.length && valid;
		
		// Read out the mode from the request.
		j = 0;
		while (data[i] != 0x00 && i < request.getLength() && j < netascii.length()) {
			mode[j++] = data[i++]; 
			if (verbose) System.out.print((char)mode[j - 1]);
		}
		
		if (verbose) System.out.println();
		
		// Valid if mode is equal to either netascii or octet in any case combination.
		valid = (new String(mode).toLowerCase().trim().equals(netascii) || new String(mode).toLowerCase().trim().equals(octet)) && valid;
		// Valid if a read/write request and terminated with 0x00.
		valid = ((data[1] == 0x01 || data[1] == 0x02) && data[i] == 0x00)  && valid;
		
		// If the packet is a valid request, start a new transfer.
		if (valid) {
			if (verbose) System.out.println("Valid request.  Starting transfer.");
			
			transfer = new Transfer(data[1] == 0x02, request, new String(file));
			transfer.start();
		} else {
			// Invalid TFTP operation requested, send error response.
			DatagramSocket sock;
			
			byte[] emsg = createErrorMsg((byte)4, error4.getBytes());
			
			if (verbose) System.out.println(error4);
			
			try {
				sock = new DatagramSocket();
				sock.send(new DatagramPacket(emsg, emsg.length, request.getAddress(), request.getPort()));	//send error
				sock.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * UI
	 * @author MatthewPenner
	 * The UI class handles user inputs. It allows the user input to be read during transfers.
	 */
	private class UI extends Thread {
		private boolean quit;
		
		public UI () {
			quit = false;
		}
		
		/**
		 * Prints out the user's options.
		 */
		private void printUI() {
			System.out.println("V - Toggle verbose mode");
			System.out.println("C - Change server directory");
			System.out.println("Q - Quit");
			System.out.print("Verbose: "); System.out.println(verbose);
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
			
				if (command.isEmpty()==false){	
					switch (command.toLowerCase().charAt(0)) {
						case 'q': quit = true;
							  quit();
							  break;
						case 'c': changeDir();
								break;
						case 'v': verbose = !verbose;
							  break;
					}
				
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
		private boolean type;
		private int port;
		private byte[] rData;
		private DatagramSocket sock;
		private DatagramPacket sPkt, rPkt;
		private InetAddress target;
		private String filename;
		
		/**
		 * Constructor for the Transfer class
		 * @param type If true, the transfer is a write request.  Else, it's a read request.
		 * @param request The request which spawned the transfer.
		 * @param filename The name of the file requested.
		 * @param verbose Indicates whether this transfer is in verbose mode.
		 */
		public Transfer(boolean type, DatagramPacket request, String filename) {
			this.type = type;
			this.filename = dir + filename.trim();
			
			target = request.getAddress();
			port = request.getPort();
			
			rData = new byte[516];
			
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
		public void receive() throws SocketTimeoutException {
			rPkt = new DatagramPacket(rData, 516);
			
			try {
				sock.receive(rPkt);
			} catch (SocketTimeoutException e) {
				throw e;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private void readReceive() {
			boolean success = false;
			
			while (!success) {
				try {
					receive();
					success = true;
				} catch (SocketTimeoutException e) {
					if (verbose) System.out.println("Receive timed out.  Retransmitting.");
					send(sPkt);
					success = false;
				}
			}
		}
		
		/**
		 * Services a write transfer.  Reads from the client over the network and writes the file locally.
		 * @throws IOException
		 */
		private void write() throws IOException {
			byte[] data;
			BufferedOutputStream out;
			
			byte[] response = new byte[4];
			byte[] block = {0x00, 0x00};
			
			// Opens the file to write.
			if (verbose) System.out.println("Opening file.");
			
			try {
				out = new BufferedOutputStream(new FileOutputStream(filename));
			} catch (FileNotFoundException e) {
				String errorMsg = "You don't have permission to write to " + filename + ".";
				byte[] msg = createErrorMsg((byte)0x02, errorMsg.getBytes());
				if (verbose) System.out.println(errorMsg);
				DatagramPacket errorPkt = new DatagramPacket(msg, msg.length, target, port);
				send(errorPkt);
				return;
			}
			
			sock.setSoTimeout(0);
			
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
				
				if (verbose && rPkt.getData()[1] == (byte)0x03) {
 					System.out.print("Received ");
 					System.out.println(new String(rPkt.getData()));
 					System.out.print(rPkt.getLength());
 					System.out.println(" bytes");
 					System.out.print("Opcode ");
 					System.out.println(new Integer(rPkt.getData()[1]));
 					System.out.print("Block ");
 					System.out.println(0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2]));
 					System.out.println();
 				}
				
				// The received packet came from an unknown source.
				if (!rPkt.getAddress().equals(target) || rPkt.getPort() != port) {
					if (verbose) System.out.println(badTID);
					
					byte[] errorData =createErrorMsg((byte)5, badTID.getBytes());
					sPkt  = new DatagramPacket(errorData, errorData.length, rPkt.getAddress(), rPkt.getPort());
					send(sPkt);
					continue;
				}
				if(!(rPkt.getData()[1] == (byte)0x03 || rPkt.getData()[1] == (byte)0x05 )){
					if (verbose) System.out.println(error4);
					
					byte[] errorData =createErrorMsg((byte)4, error4.getBytes());
					sPkt  = new DatagramPacket(errorData, errorData.length, rPkt.getAddress(), rPkt.getPort());
					send(sPkt);
					quit();
				}

				// While received error packet, handle error.
				while (rPkt.getData()[1] == (byte)0x05) {
					if (rPkt.getData()[3] == (byte)0x05) {
						if (verbose) System.out.println("Acknowledge went to incorrect client, attempting to retransfer");
						
						send(sPkt);
						receive();
						
						if ((0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2])) <= (0xff & block[1] + 256 * (0xff & block[0]) - 1)) {
							send(sPkt);
							continue;
						}
						
						// If the next packet is correct, print packet information.
						if (verbose && rPkt.getData()[1] == (byte)0x03) {
							System.out.print("Received ");
		 					System.out.println(new String(rPkt.getData()));
		 					System.out.print(rPkt.getLength());
		 					System.out.println(" bytes");
		 					System.out.print("Opcode ");
		 					System.out.println(new Integer(rPkt.getData()[1]));
		 					System.out.print("Block ");
		 					System.out.println(0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2]));
		 					System.out.println();
		 				}
					} else if (rPkt.getData()[3] == (byte)0x04) {
						// Invalid TFTP operation. Unrecoverable by definition.
						byte[] errorMsg = new byte[rPkt.getLength()];
						
						System.arraycopy(rPkt.getData(), 4, errorMsg, 0, rPkt.getLength() - 5);
						
						if (verbose) System.out.println("Error code 4, Invalid TFTP Operation");
						if (verbose) System.out.println(new String(errorMsg));
						
						quit();
					}
				}
				
				if ((0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2])) == (0xff & block[1] + 256 * (0xff & block[0]) - 1)) {
					send(sPkt);
					continue;
				}
				
				// Separate data from header.
				data = new byte[rPkt.getLength() - 4];
				System.arraycopy(rPkt.getData(), 4, data, 0, rPkt.getLength() - 4);
				
				if (verbose) {
					System.out.print("Output ");
					System.out.println(new String(data));
					System.out.println();
				}
				
				if ((0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2])) == (0xff & block[1] + 256 * (0xff & block[0]) - 1)) {
					try {
						out.write(data, 0, data.length);
					} catch (IOException e) {
						String errorMsg = "Disk full or allocation exceeded.";
						byte[] msg = createErrorMsg((byte)0x03, errorMsg.getBytes());
						if (verbose) System.out.println(errorMsg);
						DatagramPacket errorPkt = new DatagramPacket(msg, msg.length, target, port);
						send(errorPkt);
						return;
					}
					out.flush();
					
					if (block[1]++ == (byte)0xff) block[0]++;
				}
				
				// Build and send acknowledge packet.
				response = new byte[4];
				response[0] = 0x00;
				response[1] = 0x04;
				response[2] = rPkt.getData()[2];
				response[3] = rPkt.getData()[3];
				
				if (verbose) {
					System.out.print("Sending acknowledge for block ");
 					System.out.println(0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2]));
 					System.out.println();
 				}
				
				sPkt = new DatagramPacket(response, response.length, target, port);
				send(sPkt);
			} while (rPkt.getLength() == 516);
			
			out.close();
			System.out.println("Finished write.");
		}
		
		/**
		 * Services a read transfer.  Reads from local file and writes to the client over the network.
		 * @throws IOException
		 */
		private void read() throws IOException {
			int sizeRead;
			byte[] response;
			byte[] data = new byte[512];
			byte[] block = {0x00, 0x00};
			
			boolean success = false;
			
			//Opens file to read.
			if (verbose) System.out.println("Opening file.");
			BufferedInputStream in=null;
			try {
				in= new BufferedInputStream(new FileInputStream(filename));
			} catch (FileNotFoundException e) {
				String errorMsg = "The file " + filename + " could not be found.";
				byte[] msg = createErrorMsg((byte)0x01, errorMsg.getBytes());
				if (verbose) System.out.println(errorMsg);
				DatagramPacket errorPkt = new DatagramPacket(msg, msg.length, target, port);
				send(errorPkt);
				return;
			} catch (SecurityException e) {
				String errorMsg = "You don't have permission to read " + filename + ".";
				byte[] msg = createErrorMsg((byte)0x02, errorMsg.getBytes());
				if (verbose) System.out.println(errorMsg);
				DatagramPacket errorPkt = new DatagramPacket(msg, msg.length, target, port);
				send(errorPkt);
				return;
			}
			
			
			sock.setSoTimeout(timeout_ms);
			
			//Read in first 512 byte block.
			sizeRead = in.read(data);
			
			if (verbose) System.out.println("Starting read.");
		
			/*
			 * While the end of the file hasn't been reached:
			 *   - Increment the block number
			 *   - Build and send the data packet
			 *   - Wait for the acknowledge
			 *   - Read in the next 512 byte block of data
			 */
			while (sizeRead != -1) {
				if (block[1]++ == (byte)0xff) block[0]++;
				
				if (verbose) System.out.print("Read ");
				if (verbose) System.out.println(new String(data));
				
				//Build the data packet.
				response = new byte[sizeRead + 4];
				
				response[0] = 0x00;
				response[1] = 0x03;
				System.arraycopy(block, 0, response, 2, 2);
				System.arraycopy(data, 0, response, 4, sizeRead);
				
				if (verbose) {
					System.out.print("Sent ");
					System.out.println(new String(response));
					System.out.print("Opcode ");
					System.out.println(new Integer(response[1]));
					System.out.print("Block ");
 					System.out.println(0xff & block[1] + 256 * (0xff & block[0]));
					System.out.println();
				}
				
				sPkt = new DatagramPacket(response, sizeRead + 4, target, port);
				send(sPkt);
				
				while (!success) {
					readReceive();
					if (rPkt.getData()[1] != (byte) 0x04 || (rPkt.getData()[3] == block[1] && rPkt.getData()[2] == block[0])) success = true;
				}
				
				success = false;
				
				if (verbose) {
					System.out.print("Received ");
					System.out.println(new String(rPkt.getData()));
					System.out.print("Opcode ");
					System.out.println(new Integer(rPkt.getData()[1]));
					System.out.print("Block ");
 					System.out.println(0xff & rPkt.getData()[3] + 256 * (0xff & rPkt.getData()[2]));
					System.out.println();
				}
				
				//Response came from an unknown source.
				if (!rPkt.getAddress().equals(target) || rPkt.getPort() != port) {
					if (verbose) System.out.println(badTID);
					
					byte[] errorData =createErrorMsg((byte) 5, badTID.getBytes());
					sPkt  = new DatagramPacket (errorData, errorData.length, rPkt.getAddress(), rPkt.getPort());
					send(sPkt);
				}
				if(!(rPkt.getData()[1] == (byte)0x04 || rPkt.getData()[1] == (byte)0x05 )){
					if (verbose) System.out.println(error4);
					
					byte[] errorData =createErrorMsg((byte)4, error4.getBytes());
					sPkt  = new DatagramPacket(errorData, errorData.length, rPkt.getAddress(), rPkt.getPort());
					send(sPkt);
					quit();
				}

				// While received error packet, handle error.
				while (rPkt.getData()[1] == (byte)5) {
					if (rPkt.getData()[3]==(byte)5) {
						if (verbose) System.out.println("Data sent to incorrect client, attempting to retransfer");
						send(sPkt);

						while (!success) {
							readReceive();
							if (rPkt.getData()[1] != (byte) 0x04 || (rPkt.getData()[3] == block[1] && rPkt.getData()[2] == block[0])) success = true;
						}
						
						if (verbose) {
		 					System.out.print("Received ");
		 					System.out.println(new String(rPkt.getData()));
		 					System.out.print("Opcode ");
		 					System.out.println(new Integer(rPkt.getData()[1]));
		 					System.out.println();
		 				}
					} else if (rPkt.getData()[3] == (byte)0x04) {
						// Invalid TFTP operation. Unrecoverable by definition.
						byte[] errorMsg = new byte[rPkt.getLength()];
						
						System.arraycopy(rPkt.getData(), 4, errorMsg, 0, rPkt.getLength() - 5);
						
						if (verbose) System.out.println("Error code 4, Invalid TFTP Operation");
						System.out.println(new String(errorMsg));
						
						quit();
					}
					if(rPkt.getData()[3] == (byte)3)
					{
						byte[] errorMsg = new byte[rPkt.getLength()];
						
						System.arraycopy(rPkt.getData(), 4, errorMsg, 0, rPkt.getLength() - 5);
						if (verbose) System.out.println("Error code 3, disk full");
						System.out.println(errorMsg);
						return;
					}
				}
				
				sizeRead = in.read(data);
				
				if (sPkt.getLength() == 516 && sizeRead == -1) {
					data = new byte[4]; data[0] = (byte)0; data[1] = (byte)3; data[2] = block[0]; data[3] = block[1];
					sPkt = new DatagramPacket(data, 4, target, port);
					send(sPkt);
				}
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
		
		while (!server.exit) {
			server.receive();
			server.parsePacket();
		}
	}
}
