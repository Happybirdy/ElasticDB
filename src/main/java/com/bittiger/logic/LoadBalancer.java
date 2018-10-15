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
		switch (tpcw.load_balancer_type) {
		case 0:
		default:
			// Round-robin
			nextReadServer = (nextReadServer + 1) % readQueue.size();
		case 1:
			// Random
			nextReadServer = random.nextInt(readQueue.size());
		case 2:
			// Least latency
			// Todo...
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
