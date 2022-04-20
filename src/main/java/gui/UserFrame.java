package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

import org.apache.commons.lang3.StringUtils;

import model.Client;
import model.Message;
import model.Status;

import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class UserFrame extends JFrame {

	public int i = 0;
	public JPanel mainPanel;
	public JPanel insideCenter;
	public String ipAddress;
	public int port;
	private JScrollPane scroll;
	private JButton btnSend;
	private JButton btnConnect;
	private JButton btnMatch;
	private JTextArea ChatArea;
	private JTextField txMessage;
	private JTextField txName;
	private Client client;
	private volatile boolean running = true;
	Thread t;

	public UserFrame() throws UnknownHostException, IOException {
		setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (client != null) {
					try {
						running = false;
						Message send = new Message(null, null, Status.EXIT);
						System.out.println(send);
						client.sendMessage(send);
						client.closeAll();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});

		setResizable(false);
		setSize(800, 600);

		setLocationRelativeTo(null);
		setTitle("Chat App");
		mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setLayout(new BorderLayout(0, 0));
		setContentPane(mainPanel);

		ipAddress = "localhost";
		port = 666;

		insideCenter = new JPanel();
		insideCenter.setBorder(new LineBorder(Color.WHITE, 0, true));
		insideCenter.setPreferredSize(new Dimension(180, 650));
		insideCenter.setBackground(new Color(122, 172, 240));
		insideCenter.setLayout(null);
		mainPanel.add(insideCenter, BorderLayout.CENTER);

		ChatArea = new JTextArea();
		ChatArea.setEditable(false);

		scroll = new JScrollPane(ChatArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setBounds(29, 70, 619, 348);
		insideCenter.add(scroll);

		txMessage = new JTextField();
		txMessage.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(txMessage.getText().length() != 0) {
					btnSend.setEnabled(true);
				} else {
					btnSend.setEnabled(false);
				}
			}
		});
		txMessage.setBounds(29, 474, 507, 63);
		insideCenter.add(txMessage);
		txMessage.setColumns(10);

		btnSend = new JButton("Gửi");
		btnSend.setEnabled(false);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (client.isMatched()) {
						Message send = new Message(client.getName(), txMessage.getText(), Status.CHAT);
						client.sendMessage(send);
						ChatArea.append(client.getName() + " : " + txMessage.getText() + "\n");
					} else {
						System.out.println("Chưa ghép đôi mà đòi send");
					}

				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		btnSend.setBounds(563, 474, 85, 63);
		insideCenter.add(btnSend);

		JList list = new JList();
		list.setBounds(668, 70, 108, 348);
		insideCenter.add(list);

		txName = new JTextField();
		txName.setBounds(29, 21, 248, 28);
		insideCenter.add(txName);
		txName.setColumns(10);

		btnConnect = new JButton("Kết nối");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (client == null) {
						client = new Client(new Socket(ipAddress, port), txName.getText());
						Message welcome = new Message(txName.getText(), null, Status.CONNECT);
						client.sendMessage(welcome);
					} else {
						Message welcome = new Message(txName.getText(), null, Status.CONNECT);
						client.sendMessage(welcome);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				if (i == 0) {
					t = new Thread(new Runnable() {
						@Override
						public void run() {
							while (running) {
								// TODO Auto-generated method stub
								try {
									Message receivedMessage = client.receiveMessage();
									System.out.println(receivedMessage);
									switch (receivedMessage.getStatus()) {
									case MATCH:
										int action = JOptionPane.showConfirmDialog(null, "Bạn có muốn ghép đôi với "+ receivedMessage.getName() + "?",
												"Ghép đôi thành công", JOptionPane.YES_NO_OPTION);
										if (action == JOptionPane.OK_OPTION) {
											Message accept = new Message(client.getName(), null, Status.OK);
											client.sendMessage(accept);
											client.setMatched(true);
											btnMatch.setEnabled(false);
										} else {
											Message refuse = new Message(client.getName(), null, Status.REFUSE);
											client.sendMessage(refuse);
											btnMatch.setEnabled(true);
										}
										break;
									case CHAT:
										ChatArea.append(
												receivedMessage.getName() + " : " + receivedMessage.getData() + "\n");
										break;
									case EXIST:
										JOptionPane.showMessageDialog(null, "Tên trùng với người khác !", "Thông báo",
												JOptionPane.ERROR_MESSAGE);
										break;
									case UNMATCH:
										JOptionPane.showMessageDialog(null,
												"Người kia đã từ chối ghép đôi, bạn sẽ quay lại hàng chờ !",
												"Thông báo", JOptionPane.ERROR_MESSAGE);
										client.setMatched(false);
										btnMatch.setEnabled(true);
										break;
									case EXIT:
										JOptionPane.showMessageDialog(null,
												"Người kia đã thoát khỏi phòng chat, bạn sẽ quay lại hàng chờ !",
												"Thông báo", JOptionPane.ERROR_MESSAGE);
										client.setMatched(false);
										btnMatch.setEnabled(true);
										break;
									case CONNECTED:
										btnConnect.setEnabled(false);
										break;
									default:
										
									}

								} catch (IOException | ClassNotFoundException e) {
									
								}
							}
						}
					});
					t.start();
					i++;
				}
			}
		});
		btnConnect.setBounds(305, 20, 85, 30);
		insideCenter.add(btnConnect);

		btnMatch = new JButton("Ghép đôi");
		btnMatch.setEnabled(false);
		btnMatch.setBounds(400, 20, 85, 30);
		btnMatch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!client.isMatched()) {
					try {
						Message refuse = new Message(client.getName(), null, Status.MATCH);
						client.sendMessage(refuse);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			}
		});
		insideCenter.add(btnMatch);
	}
}
