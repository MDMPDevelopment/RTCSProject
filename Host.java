import java.net.DatagramSocket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;

public class Host {
	private static final int receiveLength = 100;
	
	private DatagramSocket port23, sndRcvSok, sndSok;
	private DatagramPacket rcvPkt1, rcvPkt2, sndPkt;
	private InetAddress target1, target2;
	private int returnPort;
	
	public Host() {
		try {
			port23 = new DatagramSocket(23);
			sndRcvSok = new DatagramSocket();
			target1 = InetAddress.getLocalHost();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		rcvPkt1 = new DatagramPacket(new byte[receiveLength], receiveLength);
		rcvPkt2 = new DatagramPacket(new byte[receiveLength], receiveLength);
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
	 * Receives data from the client.
	 * <p>
	 * Listens on port 23 for a request.
	 */
	public void receive1() {
		receive(rcvPkt1, port23);
	}
	
	/**
	 * Receives a reply from the server.
	 * <p>
	 * Listens for a reply on the socket used to forward data to the server.
	 */
	public void receive2() {
		receive(rcvPkt2, sndRcvSok);
	}
	
	/**
	 * Forwards the request from the client to the server.
	 * <p>
	 * Should not be called before receive1().
	 */
	public void forward() {
		//printData(rcvPkt1);
		
		sndPkt = new DatagramPacket(rcvPkt1.getData(), rcvPkt1.getData().length, target1, 69);
		// Save the client IP and port to send the server's response.
		target2 = rcvPkt1.getAddress();
		returnPort = rcvPkt1.getPort();
		
		//printData(sndPkt);
		
		send(sndPkt, sndRcvSok);
	}
	
	/**
	 * Sends the server's reply to the client.
	 * <p>
	 * Should not be called before receive2().
	 */
	public void forwardReply() {
		/*try {
			sndSok = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}*/
		
		//printData(rcvPkt2);
		
		sndPkt = new DatagramPacket(rcvPkt2.getData(), rcvPkt2.getData().length, target2, returnPort);
		
		//printData(sndPkt);
		
		send(sndPkt, sndRcvSok);
	}
	
	public static void main(String[] args) {
		Host host = new Host();
		
		while (true) {
			host.receive1();
			host.forward();
			host.receive2();
			host.forwardReply();
		}
	}
}
