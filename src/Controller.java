import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class Controller {
	private List<Bank.InitBranch.Branch.Builder> branchList;
	private Map<Bank.InitBranch.Branch, Socket> branchsocketlist;
	private static int SNAPSHOT_ID;

	public static void main(String[] args) {
		int totalMoney = 0;
		String filename=null;
		int initBal = 0;
		Controller controller = new Controller();

		Bank.InitBranch.Builder msgInitBranch = Bank.InitBranch.newBuilder();
		try {
			totalMoney = Integer.parseInt(args[0]);
			filename = args[1];
		} catch(Exception e){
			System.err.println("Invalid usage: Command ./controller <Port> <branch.txt>");
		}
		
		FileReader fr;
		try {
			fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
			String line;
			int count = 0;
			while ((line = br.readLine()) != null) {
				Bank.InitBranch.Branch.Builder branch = Bank.InitBranch.Branch.newBuilder();
				String name = line.split(" ")[0];
				String ip = line.split(" ")[1];
				int port = Integer.parseInt(line.split(" ")[2]);
				branch.setName(name);
				branch.setIp(ip);
				branch.setPort(port);
				msgInitBranch.addAllBranches(branch);
				controller.addToList(branch);
				count++;
			}
			initBal = totalMoney / count;
			
			/* Controller's Functionality */
			try {
				controller.initBranch(msgInitBranch, initBal);
				SNAPSHOT_ID=1;
				while (SNAPSHOT_ID <4) {
					System.out.println("\nsnapshot_id: "+SNAPSHOT_ID);
					Thread.sleep(3000);
					controller.initSnapshot();
					Thread.sleep(7000);
					controller.retrieveSnapshot(SNAPSHOT_ID);
					SNAPSHOT_ID++;
				}
			} catch (Exception e) {
				System.out.println("Error: All the branches are not running.\nError:"+e.getMessage());
				
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: File " + filename + " doesnt not exist.");
			System.exit(0);
		} catch (IOException e) {
			System.err.println("ERROR: Cannot read the file.");
			System.exit(0);
		}

	}

	private void addToList(Bank.InitBranch.Branch.Builder branch) {
		// TODO Auto-generated method stub
		this.branchList.add(branch);
	}

	/**
	 * Default Constructor
	 */
	public Controller() {
		// socketList = new ArrayList<Socket>();
		branchList = new ArrayList<Bank.InitBranch.Branch.Builder>();
		branchsocketlist = new LinkedHashMap<Bank.InitBranch.Branch, Socket>();
		SNAPSHOT_ID = 1;
	}

	/**
	 * Retrieve snapshot
	 * @param sid 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	private void retrieveSnapshot(int sid) throws IOException, InterruptedException {
		for(Entry<Bank.InitBranch.Branch, Socket> entry: branchsocketlist.entrySet()){
			/*
			 * for each branch..send the retieveSnap msg.
			 * read the msg..
			 * print the data
			 */
			Bank.InitBranch.Branch branch=entry.getKey();
			Socket brSocket = entry.getValue();
			
			Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();
			Bank.RetrieveSnapshot.Builder msgRetrieveSnap = Bank.RetrieveSnapshot.newBuilder();
			msgRetrieveSnap.setSnapshotId(sid);
			
			msgBranch.setRetrieveSnapshot(msgRetrieveSnap);
			msgBranch.build().writeDelimitedTo(brSocket.getOutputStream());
			Bank.BranchMessage snapshotMsg = null;
			while ((snapshotMsg = Bank.BranchMessage.parseDelimitedFrom(brSocket.getInputStream())) != null) {
				if (snapshotMsg.hasReturnSnapshot()) {
					Bank.ReturnSnapshot.LocalSnapshot ls = snapshotMsg.getReturnSnapshot().getLocalSnapshot();
					printResult(ls, branch);
					break;
				}
			}
		}
		
	}

	private void printResult(Bank.ReturnSnapshot.LocalSnapshot localSnapshot, Bank.InitBranch.Branch currBranch) {
		System.out.print(currBranch.getName()+": "+localSnapshot.getBalance()+", ");
		List<Integer> channelList = localSnapshot.getChannelStateList();
		int i=0;
		while(i < channelList.size()) {
			if(i%2==0){
				//Name of branch.
				Bank.InitBranch.Branch.Builder branch = branchList.get((i/2));
				if(branch.getName().equals(currBranch.getName())){
					i++; i++;
					continue; 
					}
				System.out.print(branch.getName()+"-->" + currBranch.getName()+": "+channelList.get(i+1)+", ");
				i++; i++;
				}
			} 
		System.out.println();
	}

	/**
	 * Start taking snapshot
	 */
	private void initSnapshot() {
		Random random = new Random();
		List<Bank.InitBranch.Branch> keys = new ArrayList<Bank.InitBranch.Branch>(branchsocketlist.keySet());
		Bank.InitBranch.Branch randomBranch = keys.get(random.nextInt(keys.size()));
		//System.out.println("InitSnapshot starts at " + randomBranch.getName());

		try {
			Socket socket = branchsocketlist.get(randomBranch);
			Bank.InitSnapshot.Builder initMsg = Bank.InitSnapshot.newBuilder();

			initMsg.setSnapshotId(SNAPSHOT_ID);
			Bank.BranchMessage.Builder message = Bank.BranchMessage.newBuilder();

			message.setInitSnapshot(initMsg);
			message.build().writeDelimitedTo(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("Please start the Branches");
			System.exit(0);
		}

	}

	/**
	 * Initialize the branches
	 * 
	 * @param initBranch
	 * @param initBal
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	private void initBranch(Bank.InitBranch.Builder initBranch, int initBal) throws UnknownHostException, IOException {
		for (Bank.InitBranch.Branch branch : initBranch.getAllBranchesList()) {
			initBranch.setBalance(initBal);
			int port = branch.getPort();
			String ip = branch.getIp();

			Socket socket = new Socket(ip, port);
			Bank.BranchMessage.Builder msgBranch = Bank.BranchMessage.newBuilder();
			msgBranch.setInitBranch(initBranch);
			msgBranch.build().writeDelimitedTo(socket.getOutputStream());
			branchsocketlist.put(branch, socket);
		}
	}
}
