package com.bittiger.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bittiger.client.ClientEmulator;
import com.bittiger.client.TPCWProperties;

public class LoadBalancer {
	private List<Server> readQueue = new ArrayList<Server>();
	private Server writeQueue = null;
	private List<Server> candidateQueue = new ArrayList<Server>();
	private int nextReadServer = 0;
	private static transient final Logger LOG = LoggerFactory
			.getLogger(LoadBalancer.class);
	private Random random = new Random();
	private TPCWProperties tpcw = null;
	private int server0Pos = 0;
	private int server1Pos = 0;
	private int server2Pos = 0;
	
	public LoadBalancer(ClientEmulator ce) {
		this.tpcw = ce.getTpcw();
		writeQueue = new Server(ce.getTpcw().writeQueue);
		for (int i = 0; i < ce.getTpcw().readQueue.length; i++) {
			readQueue.add(new Server(ce.getTpcw().readQueue[i]));
		}
		for (int i = 0; i < ce.getTpcw().candidateQueue.length; i++) {
			candidateQueue.add(new Server(ce.getTpcw().candidateQueue[i]));
		}
	}

	// there is only one server in the writequeue.
	public Server getWriteQueue() {
		return writeQueue;
	}

	public synchronized Server getNextReadServer() {
		int server0Weight = 1;
		int server1Weight = 16;
		int server2Weight = 128;
		// Assuming there are three servers to handle read requests,
		// i.e. there are three ip addresses in the readQueue array. 
		// The three servers are implemented based on ec2 t2.micro, 
		// t2.medium, and t2.xlarge, respectively. 
		// Referring to https://aws.amazon.com/ec2/instance-types/
		// the three servers have the following hardware setups:
		// +-----------+-------+-------------+
		// | Model	   |  vCPU |   Mem (GiB) |
		// | t2.micro  |   1   |     0.5     |
		// | t2.medium |   2   |     4       |
		// | t2.xlarge |   4   |     16      |
		// +-----------+-------+-------------+
		// So the weights we assigned to the three servers are 1:16:128
		switch (tpcw.load_balancer_type) {
		case 0:
		default:
			// Round-robin
			nextReadServer = (nextReadServer + 1) % readQueue.size();
			break;
		case 1:
			// Random
			nextReadServer = random.nextInt(readQueue.size());
			break;
		case 2:
			// Least latency
			// Todo...
			break;
		case 3:
			// Weighted hash
			// Todo...
			break;
		case 4: 
			// Weighted random
			int randomNum = random.nextInt(server0Weight + server1Weight + server2Weight);
			if (randomNum < server0Weight) {
				nextReadServer = 0;
			}
			else if (randomNum < server0Weight + server1Weight) {
				nextReadServer = 1;
			}
			else {
				nextReadServer = 2;
			}
			break;
		case 5:
			// Weighted round-robin
			switch(nextReadServer) {
			case 0:
				// It means last time the returned server is server 0.
				if (server0Pos < server0Weight) {
					server0Pos++;
				}
				else {
					nextReadServer = (nextReadServer + 1) % readQueue.size();
					server0Pos = 0;
					server1Pos++;
				}
				break;
			case 1:
				if (server1Pos < server1Weight) {
					server1Pos++;
				}
				else {
					nextReadServer = (nextReadServer + 1) % readQueue.size();
					server1Pos = 0;
					server2Pos++;
				}
				break;
			case 2:
				if (server2Pos < server2Weight) {
					server2Pos++;
				}
				else {
					nextReadServer = (nextReadServer + 1) % readQueue.size();
					server2Pos = 0;
					server0Pos++;
				}
				break;
			}
			break;
		}
		
		Server server = readQueue.get(nextReadServer);
		LOG.debug("choose read server as " + server.getIp());
		return server;
	}

	public synchronized void addServer(Server server) {
		readQueue.add(server);
	}

	public synchronized Server removeServer() {
		Server server = readQueue.remove(readQueue.size() - 1);
		candidateQueue.add(server);
		return server;
	}

	public synchronized List<Server> getReadQueue() {
		return readQueue;
	}

	// readQueue is shared by the UserSessions and Executor.
	// However, candidateQueue is only called by Executor.
	public List<Server> getCandidateQueue() {
		return candidateQueue;
	}

}
