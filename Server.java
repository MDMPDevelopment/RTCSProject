import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;

public class Server {
	private static final int receiveLength = 100;
	private static final int netasciiLength = 8;
	
	private DatagramSocket port69, sndSok;
	private DatagramPacket sPkt, rPkt;
	private byte[] mode;
	
	private Boolean valid;
	
	public Server() {
		try {
			port69 = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		mode = new byte[netasciiLength];
		rPkt = new DatagramPacket(new byte[receiveLength], receiveLength);
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
		try {
			port69.receive(rPkt);
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
		byte[] data = rPkt.getData();
		int i = 2;
		int j = 0;
		
		valid = data[0] == 0x00;
		valid = (data[1] == 0x01 || data[1] == 0x02) && valid;
		
		while (data[i] != 0x00 && i++ < data.length) {
		}
		
		valid = i > 2 && i++ < data.length && valid;
		
		while (data[i] != 0x00 && i < data.length && j < netasciiLength) {
			mode[j++] = data[i++]; 
		}
		
		valid = (new String(mode).toLowerCase().equals("netascii") || new String(mode).toLowerCase().equals("octet")) && valid;
		valid = data[i] == 0x00 && valid;
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
	public void buildResponse() throws IOException {
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
	}
	
	/**
	 * Sends the response.
	 * <p>
	 * Should not be called before buildResponse().
	 */
	public void send() {
		try {
			sndSok = new DatagramSocket();
			sndSok.send(sPkt);
			sndSok.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Server server = new Server();
		
		while (true) {
			server.receive();
			server.parsePacket();
			try {
				server.buildResponse();
			} catch (IOException e) {
				System.exit(-1);
			}
			server.send();
		}
	}
}
