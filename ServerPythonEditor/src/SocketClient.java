import java.io.PrintStream;
import java.net.Socket;


public class SocketClient {
	private String host;
	private int port;
	private String pass;
	private Socket socket;
	private PrintStream out;

	public SocketClient(String host, int port, String pass) {
		this.host = host;
		this.port = port;
		this.pass = pass;
	}
	
	public boolean connect() {
		try {
			socket = new Socket(host, port);
			out = new PrintStream(socket.getOutputStream());
			Thread.sleep(300);
			out.println(pass);
			String buffer = getAllOutput(300);
			if (!buffer.contains("Welcome!"))
				throw new Exception("Incorrect password");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public String getAllOutput() {
		return getAllOutput(300);
	}
	
	public String getAllOutput(long waiting) {
		String buffer = "";
		try {
			Thread.sleep(waiting);
			while (socket.getInputStream().available() > 0) {
				byte[] byteBuffer = new byte[socket.getInputStream().available()];
				socket.getInputStream().read(byteBuffer);
				buffer += new String(byteBuffer);
				Thread.sleep(waiting);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buffer;
	}
	
	public void send(String code) {
		out.println(code);
	}
}
