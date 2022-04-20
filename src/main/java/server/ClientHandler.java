package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import model.Message;
import model.Status;


public class ClientHandler extends Thread {
	private final Socket socket;
	private final ObjectInputStream serverInput;
	private final ObjectOutputStream serverOutput;
	private volatile boolean running = true;
	private List<String> rejected = new ArrayList<>();
	private String username;
	private String peerInfo;

	public ClientHandler(Socket socket, ObjectInputStream serverInput, ObjectOutputStream serverOutput) {
		this.socket = socket;
		this.serverInput = serverInput;
		this.serverOutput = serverOutput;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPeerInfo() {
		return peerInfo;
	}

	public void setPeerInfo(String peerInfo) {
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		Message received;
		try (socket; serverInput; serverOutput;) {
			while (running) {
				received = (Message) serverInput.readObject();
				System.out.println(received);
				switch(received.getStatus()) {
				case CHAT:
					if (peerInfo != null && !peerInfo.isEmpty()) {
						Server.listMatched.get(peerInfo).sendMessage(received);
					}
					break;
				case OK:
					//peerInfo = received.getName();
					break;
				case EXIT:
					if(peerInfo == null) {
						Server.listWait.remove(username);
					} else {
						ClientHandler b;
						if(Server.listMatched.get(peerInfo) != null) {
							b = Server.listMatched.get(peerInfo);
							Message unmatch = new Message(username,null,Status.EXIT);
							b.sendMessage(unmatch);
							System.out.println("gửi message từ chối");
						}
						removeMatched();
						Server.listMatched.remove(username);
						Server.listWait.remove(username);
					}
					
					running = false;
					break;
				case REFUSE:
					ClientHandler b;
					rejected.add(peerInfo);
					if(Server.listMatched.get(peerInfo) != null) {
						b = Server.listMatched.get(peerInfo);
					} else {
						b = Server.listWait.get(peerInfo);
					}
					Message unmatch = new Message(username,null,Status.UNMATCH);
					b.sendMessage(unmatch);
					System.out.println("gửi message từ chối");
					removeMatched();
					if(Server.listWait.size()-1 > rejected.size() && Server.duaVaoSau.equals(username) ) {
						matching();
					}
					
					break;
				case MATCH:
					matching();
					break;
				default:
					if(Server.listMatched.get(received.getName()) != null || Server.listWait.get(received.getName()) != null) {
						Message mess = new Message(username,null,Status.EXIST);
						sendMessage(mess);
					} else if(username == null) {
						username = received.getName();
						Server.listWait.put(username, this);
						Message welcome = new Message(username,null,Status.CONNECTED);
						System.out.println(welcome);
						sendMessage(welcome);
						Server.duaVaoSau = username;
						matching();
					}
				}
			}
			System.out.println("ClientHanlder :" + username + " đã tắt !");
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		}

	}
	
	public void sendMessage(Message mess) throws IOException {
		serverOutput.writeObject(mess);
		serverOutput.flush();
	}
	
	public void matching() throws IOException, InterruptedException {
		System.out.println("Vào ghép đôi");
		System.out.println(username);
		synchronized(this) {
			if(Server.listWait.size()>1) {
				Server.listWait.remove(username);
            	ArrayList<ClientHandler> list = new ArrayList<>(Server.listWait.values());
            	list.removeIf(x -> rejected.contains(x.getUsername()));
            	Collections.shuffle(list);
            	ClientHandler benB = list.get(0);
            	setPeerInfo(benB.getUsername());
            	benB.setPeerInfo(username);
            	Server.listMatched.put(username, this);
            	Server.listMatched.put(benB.getUsername(), benB);
            	Message mess = new Message(peerInfo,null,Status.MATCH);
            	sendMessage(mess);
            	mess.setName(username);
            	benB.sendMessage(mess);
            	Server.listWait.remove(benB.getUsername());
            	System.out.println("Ghép đôi thành công");
            }
		}
	}
	
	public void removeMatched() {
		if(Server.listMatched.get(peerInfo) != null) {
			ClientHandler benB = Server.listMatched.get(peerInfo);
			Server.listMatched.remove(peerInfo);
			Server.listWait.put(peerInfo, benB);
			Server.listMatched.remove(username);
			Server.listWait.put(username, this);
		}
		setPeerInfo(null);
	}
	
}
