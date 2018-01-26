import java.net.ServerSocket;
import java.net.Socket;

public class Receiver extends Thread{
	Branch branch;
	
	public Receiver(Branch receiverBranch) {
		this.branch =receiverBranch;
	}
	
	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {	
			serverSocket = new ServerSocket(branch.ownBranch.getPort());
			Socket cntSocket = serverSocket.accept();
			ReceiverHandler cntHandler = new ReceiverHandler(cntSocket,branch);
			Thread cntThread = new Thread(cntHandler);
			cntThread.start();
			
			while (true) {
				Socket socket = serverSocket.accept();
				ReceiverHandler recHandler = new ReceiverHandler(socket,branch);
				Thread th = new Thread(recHandler);
				th.start();
			}
		} catch (Exception e) {
			System.err.println("Error occured in Receiver");
			System.exit(0);
		}
	}
}
