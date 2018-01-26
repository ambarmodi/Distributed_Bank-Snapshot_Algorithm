import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class Sender extends Thread {
	Branch branch;
	
	public Sender(Branch senderBranch) {
		this.branch = senderBranch;
	}

	@Override
	public void run() {
		try {
			establishSockets();
			while(true) {
				Random random = new Random();
				int index = random.nextInt(5);
				try {
					Thread.sleep(index*1000);
				} catch (InterruptedException e) {
					System.err.println("Error: Thread Interrupted!");
				}
				sendMoney();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This establish the sockets before hand.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void establishSockets() throws UnknownHostException, IOException {
		for (Bank.InitBranch.Branch br : branch.branchList) {
			if (!isBranchSame(br, this.branch.ownBranch)) {
				Socket socket = new Socket(br.getIp(), br.getPort());
				Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();
				Bank.Transfer.Builder transferMsg = Bank.Transfer.newBuilder();
				transferMsg.setMoney(-10);
				message.setTransfer(transferMsg);
				message.build().writeDelimitedTo(socket.getOutputStream());
				socket.getOutputStream().write(br.getName().getBytes());
				branch.outBranchSocket.put(br, socket);
			}
		}
	}

	private boolean isBranchSame(Bank.InitBranch.Branch branch, Bank.InitBranch.Branch.Builder ownBranch) {
		if ((branch.getName().equals(ownBranch.getName())) && (branch.getIp() == ownBranch.getIp())) {
			return true;
		}
		return false;
	}

	private void sendMoney() throws UnknownHostException, IOException {		
		Random random = new Random();
		int index = random.nextInt(branch.getBranchList().size());
		Bank.InitBranch.Branch destBranch = branch.getBranchList().get(index);
		
		while (isBranchSame(destBranch, branch.ownBranch)) {
			index = random.nextInt(branch.getBranchList().size());
			destBranch = branch.getBranchList().get(index);
		}
		Socket socket = branch.outBranchSocket.get(destBranch);
		Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();
		Bank.Transfer.Builder transferMsg = Bank.Transfer.newBuilder();

		int perToDeduct = random.nextInt(5);
		int transferAmt = (branch.getBalance() *perToDeduct)/100;
		if(transferAmt <=0){
			return;
		}
		transferMsg.setMoney(transferAmt);
		message.setTransfer(transferMsg);
		branch.setBalance(branch.getBalance() - transferAmt);
		
		/*System.out.println("Sending to --> " + destBranch.getName() + " --> $" + transferMsg.getMoney()
		+" Balance:"+this.branch.getBalance()); */

		message.build().writeDelimitedTo(socket.getOutputStream());
		socket.getOutputStream().write(branch.ownBranch.getName().getBytes());
	}
	

}
