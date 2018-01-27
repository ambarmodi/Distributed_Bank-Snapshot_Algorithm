# Implementation of Chandy-Lamport snapshot algorithm
-----------------------------------------------------------------------
### This is the implementation of Distributed bank having multiple branches. The controller in bank uses Chandy-Lamport global snapshot algorithm take global snapshots of your bank  which contain both the local state of each branch (i.e., its balance) and the amount of money in transit on all communication channels.
Note: This project uses Google’s Protocol Buffer for marshalling and unmarshalling messages and use TCP sockets for sending and receiving these messages. Refer https://developers.google.com/protocol-buffers/ for details.

-----------------------------------------------------------------------
## Overview:
#### Controller:
Controller is the thread responsible to set a branch’s initial balance and notify every branch of all branches in the distributed bank. The controller takes two command line inputs: the total amount of money in the distributed bank and a local file that stores the names, IP addresses, and port numbers of all branches.
An example of how the controller program should operate is provided below:
$> ./controller 4000 branches.txt

The file (branches.txt) should contain a list of names, IP addresses, and ports, in the format “<name> <public-ip-address> <port>”, of all of the running branches.
#### For example, if four branches with names: “branch1”, “branch2”, “branch3”, and “branch4” are running on server 128.226.180.163 on port 9090, 9091, 9092, and 9093, then branches.txt should contain:
branch1 128.226.180.163 9090
branch2 128.226.180.165 9091
branch3 128.226.180.163 9092
branch4 128.226.180.166 9093
The controller will distribute the total amount of money evenly among all branches, e.g., in the example above, every branch will receive $1,000 initial balance. 

#### Branch:
The distributed bank has multiple branches. Every branch knows about all other branches. A single TCP connection is setup between every pair of branches. Each branch starts with an initial balance (allocated by the controller). The branch then randomly selects another destination branch and sends a random amount of money to this destination branch at unpredictable times.

-----------------------------------------------------------------------
## Implementation details:
Chandy-Lamport global snapshot algorithm take global snapshots of your bank. In case of the distributed bank, a global snapshot will contain both the local state of each branch (i.e., its balance) and the amount of money in transit on all communication channels. Each branch will be responsible for recording and reporting its own local state (balance) as well as the total money in transit on each of its incoming channels.
For simplicity, in this assignment, the controller will contact one of the branches to initiate the global snapshot. It does so by sending a message indicating the InitSnapshot operation to the selected branch. The selected branch will then initiate the snapshot by first recording its own local state and send out Marker messages to all other branches. After some time (long enough for the snapshot algorithm to finish), the controller sends
RetrieveSnapshot messages to all branches to retrieve their recorded local and channel states. 
#### If the snapshot is correct, the total amount of money in all branches and in transit should equal to the command line argument given to the controller.

-----------------------------------------------------------------------
#### Files in the project:
1. Makefile  (To build the application)
2. branch.sh  (This will start the branch server)
3. controller.sh (This will start the controller)
4. src folder which includes the java deliverable as well as the Bank.java generated from protobuf

#### Java file Description:
1. Controller.Java (present in src/)
* This is responsible to initialize the snapshot.

2. Branch.java (present in src/)
* This is called when branch server is started. Each branch is both sender and receiver which are two different threads responsible fpr sending and receiving money.
* Receiver thread is also responsible for handling the marker messages and for returning the snapshot to the controller.
* Each connection between the branch is Single TCP connection which is handle by the seperate thread.

-----------------------------------------------------------------------
### Instructions to execute.
1. make 												(To compile)
2. ./branch.sh <BRANCH_NAME> <port_no>					(start Branch server on mentioned port)
3. ./controller <TOTAL_AMOUNT> <Branch_list.txt>		(Specify the txt file containing branch details)
4. CTRL + C   											(To terminate the server)

Note: Branch name provided to branch.sh must match with <Branch_list.txt>. In case it doesnot match, Branch server takes the name of the branch from branches.txt provided to controller.

-----------------------------------------------------------------------
### Sample Input/Output:

#### BRANCH:
$: ./branch.sh brach2 9002

====================Server Details====================
Server Machine: 128.226.180.167
Port number: 9002


#### CONTROLLER: NOTE: Controller should start after the branches are started.
$: ./controller.sh 3000 branches.txt
snapshot_id: 1
branch1: 932, branch2-->branch1: 10, branch3-->branch1: 0,
branch2: 1010, branch1-->branch2: 0, branch3-->branch2: 18,
branch3: 1030, branch1-->branch3: 0, branch2-->branch3: 0,

-----------------------------------------------------------------------
