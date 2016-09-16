package nnode;

public class LobbyMessage {

	
	protected WebPlayer sender;
	protected String message;
	public LobbyMessage(WebPlayer np, String message) {
		this.sender = np;
		this.message = message;
	}
	public String access(WebPlayer accessor) {
		StringBuilder sb = new StringBuilder();
    	sb.append("<b>");
		if(accessor.equals(sender))
			sb.append("You");
		else
			sb.append(sender.name);
		sb.append("</b> : " + message);
		return sb.toString();
	}

	
}
