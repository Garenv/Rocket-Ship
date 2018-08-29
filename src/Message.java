import java.io.*;

public class Message implements Serializable {
	protected static final long serialVersionUID = 1112122200L;

	public static final int stationOpened = 0,
			fillingUpResevoir = 1,
			takeOffTime = 2;

	public static final int controller = 50, shuttle = 51, supervisor = 52;
	private int type;
	private String message;
	private int toWhome;
	private Object data;
	
	public Message(int type, String message,int toWhome,Object data) {
		this.type = type;
		this.message = message;
		this.toWhome = toWhome;
		this.data = data;
	}
	
	public int getToWhome() {
		return toWhome;
	}
	
	// Getters
	int getType() {
		return type;
	}
	
	String getMessage() {
		return message;
	}

	public Object getData() {
		return data;
	}
}