package agent.logging.instrumentation;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {

	@Override
	public String format(LogRecord msg) {
		String text = msg.getLevel() + ": " + msg.getMessage();
		if(msg.getThrown() != null) {
			text += msg.getThrown();
		}
		return text + System.lineSeparator();
	}
}
