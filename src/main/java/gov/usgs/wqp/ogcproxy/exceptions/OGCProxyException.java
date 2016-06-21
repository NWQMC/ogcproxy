package gov.usgs.wqp.ogcproxy.exceptions;

public class OGCProxyException extends Exception {
	private static final long serialVersionUID = 150502219018410239L;
	
	private final OGCProxyExceptionID exceptionid;		// uniquely generated id for this exception
	private final String classname; 					// the name of the class that threw the exception
	private final String method;						// the name of the method that threw the exception
	private final String message;						// a detailed message
	private final OGCProxyException previous; 			// the exception which was caught
	private String delimeter = "\n";					// line separator

	public OGCProxyException(final OGCProxyExceptionID id, final String classname, final String method, final String message) {
		this(id, classname, method, message, null);
	}
	public OGCProxyException(final OGCProxyExceptionID id, final String classname, final String method, final String message, Exception cause) {
		super(message, cause);
		this.exceptionid	= id;
		this.classname		= classname;
		this.method			= method;
		this.message		= message;
		this.previous		= null;
	}

	public OGCProxyException(final OGCProxyExceptionID id, final String classname, final String method, final String message, final OGCProxyException previous) {
		this.exceptionid	= id;
		this.classname		= classname;
		this.method			= method;
		this.message		= message;
		this.previous		= previous;
	}

	public String traceBack() {
		return traceBack("\n");
	}

	public String traceBack(final String sep) {
		this.delimeter = sep;
		int level = 0;
		OGCProxyException e = this;
		final StringBuffer text = new StringBuffer(line("OGCProxyException Trace: Calling sequence (top to bottom)"));
		while (e != null) {
			level++;
			text.append(this.delimeter);
			text.append(line("--level " + level + "--------------------------------------"));
			text.append(line("Class/Method	: " + e.classname + "/" + e.method));
			text.append(line("Id			: " + e.exceptionid));
			text.append(line("Message		: " + e.message));
			e = e.previous;
		}
		return text.toString();
	}

	private String line(final String s) {
		return s + this.delimeter;
	}

	@Override
	public String getMessage() {
		return this.traceBack();
	}

	@Override
	public String toString() {
		return this.traceBack();
	}

	public OGCProxyExceptionID getExceptionid() {
		return this.exceptionid;
	}

	public String getClassname() {
		return this.classname;
	}

	public String getMethod() {
		return this.method;
	}

	public OGCProxyException getPrevious() {
		return this.previous;
	}

	public String getMessageOnly() {
		return this.message;
	}
}
