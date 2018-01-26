import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

public class ReceiverHandler implements Runnable{
	private Socket socket;
	Branch branch;
	boolean isChannelRecording;
	
	public ReceiverHandler(Socket socket, Branch branch){
		this.branch = branch;
		this.socket=socket;
		isChannelRecording=false;
	}
	
	@Override
	public void run() {
		try {
			handleRequest();
		} catch (Exception e) {
			//Eating the exception in thread.
			//e.printStackTrace();
		}
	}
	
	private void handleRequest() throws Exception, InterruptedException {
		InputStream ios = socket.getInputStream();
		Bank.BranchMessage msg = null;
		while ((msg = Bank.BranchMessage.parseDelimitedFrom(ios)) != null) {
			if (msg.hasInitBranch()) {
				initializeBranch(msg);
				branch.startSender();
			}
			if (msg.hasTransfer()) {
				receiveMoney(msg);
			}
			if (msg.hasInitSnapshot()) {
				int snapshotID = msg.getInitSnapshot().getSnapshotId();
				//System.out.println("Snapshot statred for ID" + snapshotID);
				branch.currSnapshotId=snapshotID;
				//This act as First marker msg 
				isChannelRecording=false;
				branch.isRecording = true;
				putLocalSnapshot(snapshotID);
				sendMarkerMsgs(snapshotID);
			}
			if (msg.hasRetrieveSnapshot()) {
				int snapshotID = msg.getRetrieveSnapshot().getSnapshotId();
				//System.out.println("RetrieveSnapshot statred for ID" + snapshotID);
				retrieveSnapshot(snapshotID);
			}
			if (msg.hasMarker()) {
				receivedMarkerMsg(msg);
			}
		}
	}
	
	private void retrieveSnapshot(int snapshotID) throws Exception {
		if(branch.markerMsg.containsKey(snapshotID)){
			Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = branch.markerMsg.get(snapshotID);
			
			Bank.BranchMessage.Builder msg = Bank.BranchMessage.newBuilder();
			Bank.ReturnSnapshot.Builder  returnMsg= Bank.ReturnSnapshot.newBuilder();
			returnMsg.setLocalSnapshot(localSnapshot);
			msg.setReturnSnapshot(returnMsg);
			msg.build().writeDelimitedTo(socket.getOutputStream());
			
		} else{
			//System.out.println("ERROR: initSnapshot not completed for SID:"+ snapshotID);
		}
		
		
	}

	private void sendMarkerMsgs(int snapshotID) throws IOException {
		for (Bank.InitBranch.Branch br : branch.getBranchList()) {
			if(!isBranchSame(br, branch.ownBranch)) {
				//Send the marker message
				Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();
				Bank.Marker.Builder marker = Bank.Marker.newBuilder();
				marker.setSnapshotId(snapshotID);
				msgBranch.setMarker(marker);
				msgBranch.build().writeDelimitedTo(branch.outBranchSocket.get(br).getOutputStream());
				//System.out.println("Marker send to --> "+ br.getName());
				//Sending the src name after marker..
				branch.outBranchSocket.get(br).getOutputStream().write(branch.ownBranch.getName().getBytes());
			}
		}
	}

	private void receiveMoney(Bank.BranchMessage msg) throws IOException {
		int money = msg.getTransfer().getMoney();
		if (money < 0) {
			//Establish socket connection and save the sockets for reuse.
			byte[] bName = new byte[255];
			int len = socket.getInputStream().read(bName);
			String name = new String(bName, 0, len);
			Bank.InitBranch.Branch br = findBranch(name, branch.branchList);
			branch.inSocketBranch.put(socket, br);
			
		} else {
			//Get dest name
			byte[] bName = new byte[255];
			int len = socket.getInputStream().read(bName);
			String name = new String(bName, 0, len);
			Bank.InitBranch.Branch destBr = findBranch(name, branch.branchList);
			this.branch.setBalance(this.branch.getBalance() + money);
			if(!isChannelRecording){
				//Channel is not recording...
				//System.out.println("Money recieved from: "+ destBr.getName() +" Balance:" + branch.getBalance());
			} else{
				int destBranchindex = getBranchIndex(destBr);
				//As array stored as first index and 2nd value
				destBranchindex = 2*destBranchindex;
				
				Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = branch.markerMsg.get(branch.currSnapshotId);
				int updatedAmt = localSnapshot.getChannelState(destBranchindex+1) + money;
				localSnapshot.setChannelState(destBranchindex, 1);  //1 denotes incoming channel
				localSnapshot.setChannelState(destBranchindex+1, updatedAmt); //this denoted amount on that channel
				localSnapshot.setBalance(branch.getBalance());
			}
		}
	}

	private Bank.InitBranch.Branch findBranch(String name, List<Bank.InitBranch.Branch> branchList) {
		for(Bank.InitBranch.Branch br : branchList){
			if(name.equals(br.getName())){
				return br;
			}
		}
		return null;
	}

	private void initializeBranch(Bank.BranchMessage msg) {
		this.branch.setBranchList(msg.getInitBranch().getAllBranchesList());
		this.branch.setBalance(msg.getInitBranch().getBalance());
		//This loop only detects its ownBranch
		for (Bank.InitBranch.Branch br : branch.getBranchList()) {
			if (br.getPort() == this.branch.ownBranch.getPort()
					&& br.getIp().equals(this.branch.ownBranch.getIp())) {
				this.branch.setOwnBranch(br.toBuilder());
			}
		}
		//System.out.println("InitBranch: Balance:" + this.branch.getBalance());
		makeThreadsleep(500);
	}

	
	private void makeThreadsleep(int sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void receivedMarkerMsg(Bank.BranchMessage msg) throws IOException, InterruptedException {
		int sid = msg.getMarker().getSnapshotId();
		//Below snippet reads the src branch name for marker
		byte[] bName = new byte[255];
		int len = socket.getInputStream().read(bName);
		String name = new String(bName, 0, len);

		Bank.InitBranch.Branch br = findBranch(name, branch.branchList);
		
		if(branch.markerMsg.containsKey(sid)){
			isChannelRecording =false;
			//System.out.println("Not first Marker msg. Received from:" + br.getName());
		}
		else {
			//System.out.println("First Marker msg. Receviced from: "+ br.getName());
			isChannelRecording=false;
			branch.isRecording = true;
			branch.currSnapshotId=sid;
			//Mark the incooming as 0;
			putLocalSnapshot(sid);
			
			int destBranchindex = getBranchIndex(br);
			//As array stored as first index and 2nd value
			destBranchindex = 2*destBranchindex;
			branch.markerMsg.get(sid).setChannelState(destBranchindex+1, 0);
			/*synchronized (branch.sender) {
				branch.sender.wait();
				sendMarkerMsgs(sid);
				branch.sender.notify();
			}*/
			sendMarkerMsgs(sid);
			
			
		
		}
	} 
	
	private void putLocalSnapshot(int sid) {
		Bank.ReturnSnapshot.LocalSnapshot.Builder localSnapshot = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();
		localSnapshot.setSnapshotId(sid);
		localSnapshot.setBalance(branch.getBalance());
		//Make the channel state array long enough..
		for(int i=0;i<2* (branch.getBranchList().size()); i++) {
			localSnapshot.addChannelState(0);
		}
		branch.markerMsg.put(sid, localSnapshot);		
	}

	private int getBranchIndex(Bank.InitBranch.Branch br) {
		int index=0;
		for(Bank.InitBranch.Branch brInd : branch.branchList){
			if(br.getName().equals(brInd.getName())){
				return index;
			} else {
				index++;
			}
		}
		return index;
	}

	private boolean isBranchSame(Bank.InitBranch.Branch branch, Bank.InitBranch.Branch.Builder ownBranch) {
		if ((branch.getName().equals(ownBranch.getName())) && (branch.getIp() == ownBranch.getIp())) {
			return true;
		}
		return false;
	}

}
