import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ambarmodi
 *
 */
public class Branch {
	public Sender sender;
	public Receiver receiver;
	
	private int balance;
	public Bank.InitBranch.Branch.Builder ownBranch;
	public List<Bank.InitBranch.Branch> branchList;
	
	public boolean isRecording;
	public int currSnapshotId;
	public Map<Integer,Bank.ReturnSnapshot.LocalSnapshot.Builder> markerMsg;
	
	public Map<Socket, Bank.InitBranch.Branch> inSocketBranch;
	public Map<Bank.InitBranch.Branch, Socket> outBranchSocket;
	
	public Branch(int port, String ip){
		ownBranch = Bank.InitBranch.Branch.newBuilder();
		ownBranch.setIp(ip);
		ownBranch.setPort(port);
		inSocketBranch = new LinkedHashMap<Socket, Bank.InitBranch.Branch>();
		outBranchSocket = new LinkedHashMap<Bank.InitBranch.Branch, Socket>();
		markerMsg = new HashMap<Integer, Bank.ReturnSnapshot.LocalSnapshot.Builder>();;
		isRecording = false;
	}
	
	public static void main(String[] args) {
		try {
			String name = args[0];
			int port = Integer.parseInt(args[1]);
			String ip = InetAddress.getLocalHost().getHostAddress();

			Branch branch = new Branch(port, ip);
			System.out.println("\n====================Server Details====================");
			System.out.println("Server Machine: " + ip);
			System.out.println("Port number: " + port);
			System.out.println();
			
			branch.startReciever();
		
		} catch (UnknownHostException e1) {
			System.err.println("Exception: UnknownHost Error");
		}
	}

	/**
	 * @return the balance
	 */
	public int getBalance() {
		return balance;
	}

	/**
	 * @param balance the balance to set
	 */
	public synchronized void setBalance(int balance) {
		this.balance = balance;
	}

	/**
	 * @return the ownBranch
	 */
	public Bank.InitBranch.Branch.Builder getOwnBranch() {
		return ownBranch;
	}

	/**
	 * @param ownBranch the ownBranch to set
	 */
	public void setOwnBranch(Bank.InitBranch.Branch.Builder ownBranch) {
		//TODO: remove itself from brachList
		/*for(Bank.InitBranch.Branch branch : branchList) {
			if(branch.getIp().equals(ownBranch.getIp()) && branch.getPort() == ownBranch.getPort())
		}*/
		this.ownBranch = ownBranch;
	}

	/**
	 * @return the branchList
	 */
	public List<Bank.InitBranch.Branch> getBranchList() {
		return branchList;
	}

	/**
	 * @param branchList the branchList to set
	 */
	public void setBranchList(List<Bank.InitBranch.Branch> branchList) {
		this.branchList = branchList;
	}

	private void startReciever() {
		receiver = new Receiver(this);
		receiver.start();
	}
	
	public void startSender() {
		//Bank.InitBranch senderBranch = this.ownBranch;
		sender = new Sender(this);
		sender.start();
	}
}
