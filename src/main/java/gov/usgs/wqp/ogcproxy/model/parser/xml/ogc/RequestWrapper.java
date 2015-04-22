package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

public class RequestWrapper implements HttpServletRequest {
	private HttpServletRequest delegate;
	private String postBodySansSearchParams;
	
	
	public RequestWrapper(HttpServletRequest request, String postBodySansSearchParams) {
		this.delegate = request;
		this.postBodySansSearchParams = postBodySansSearchParams;
	}

	
	public HttpServletRequest getDelegate() {
		return delegate;
	}

	public String getPostBodySansSearchParams() {
		return postBodySansSearchParams;
	}
	

	public Object getAttribute(String name) {
		return delegate.getAttribute(name);
	}

	public String getAuthType() {
		return delegate.getAuthType();
	}

	public Cookie[] getCookies() {
		return delegate.getCookies();
	}

	public Enumeration<String> getAttributeNames() {
		return delegate.getAttributeNames();
	}

	public long getDateHeader(String name) {
		return delegate.getDateHeader(name);
	}

	public String getCharacterEncoding() {
		return delegate.getCharacterEncoding();
	}

	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
		delegate.setCharacterEncoding(env);
	}

	public int getContentLength() {
		return delegate.getContentLength();
	}

	public String getHeader(String name) {
		return delegate.getHeader(name);
	}

	public long getContentLengthLong() {
		return delegate.getContentLengthLong();
	}

	public Enumeration<String> getHeaders(String name) {
		return delegate.getHeaders(name);
	}

	public String getContentType() {
		return delegate.getContentType();
	}

	public ServletInputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	public String getParameter(String name) {
		return delegate.getParameter(name);
	}

	public Enumeration<String> getHeaderNames() {
		return delegate.getHeaderNames();
	}

	public int getIntHeader(String name) {
		return delegate.getIntHeader(name);
	}

	public Enumeration<String> getParameterNames() {
		return delegate.getParameterNames();
	}

	public String getMethod() {
		return delegate.getMethod();
	}

	public String[] getParameterValues(String name) {
		return delegate.getParameterValues(name);
	}

	public String getPathInfo() {
		return delegate.getPathInfo();
	}

	public Map<String, String[]> getParameterMap() {
		return delegate.getParameterMap();
	}

	public String getPathTranslated() {
		return delegate.getPathTranslated();
	}

	public String getProtocol() {
		return delegate.getProtocol();
	}

	public String getScheme() {
		return delegate.getScheme();
	}

	public String getContextPath() {
		return delegate.getContextPath();
	}

	public String getServerName() {
		return delegate.getServerName();
	}

	public int getServerPort() {
		return delegate.getServerPort();
	}

	public BufferedReader getReader() throws IOException {
		return delegate.getReader();
	}

	public String getQueryString() {
		return delegate.getQueryString();
	}

	public String getRemoteUser() {
		return delegate.getRemoteUser();
	}

	public String getRemoteAddr() {
		return delegate.getRemoteAddr();
	}

	public String getRemoteHost() {
		return delegate.getRemoteHost();
	}

	public boolean isUserInRole(String role) {
		return delegate.isUserInRole(role);
	}

	public void setAttribute(String name, Object o) {
		delegate.setAttribute(name, o);
	}

	public Principal getUserPrincipal() {
		return delegate.getUserPrincipal();
	}

	public void removeAttribute(String name) {
		delegate.removeAttribute(name);
	}

	public String getRequestedSessionId() {
		return delegate.getRequestedSessionId();
	}

	public Locale getLocale() {
		return delegate.getLocale();
	}

	public String getRequestURI() {
		return delegate.getRequestURI();
	}

	public Enumeration<Locale> getLocales() {
		return delegate.getLocales();
	}

	public boolean isSecure() {
		return delegate.isSecure();
	}

	public StringBuffer getRequestURL() {
		return delegate.getRequestURL();
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return delegate.getRequestDispatcher(path);
	}

	public String getServletPath() {
		return delegate.getServletPath();
	}

	@SuppressWarnings("deprecation")
	public String getRealPath(String path) {
		return delegate.getRealPath(path);
	}

	public HttpSession getSession(boolean create) {
		return delegate.getSession(create);
	}

	public int getRemotePort() {
		return delegate.getRemotePort();
	}

	public String getLocalName() {
		return delegate.getLocalName();
	}

	public String getLocalAddr() {
		return delegate.getLocalAddr();
	}

	public int getLocalPort() {
		return delegate.getLocalPort();
	}

	public ServletContext getServletContext() {
		return delegate.getServletContext();
	}

	public HttpSession getSession() {
		return delegate.getSession();
	}

	public AsyncContext startAsync() throws IllegalStateException {
		return delegate.startAsync();
	}

	public String changeSessionId() {
		return delegate.changeSessionId();
	}

	public boolean isRequestedSessionIdValid() {
		return delegate.isRequestedSessionIdValid();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return delegate.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromURL() {
		return delegate.isRequestedSessionIdFromURL();
	}

	@SuppressWarnings("deprecation")
	public boolean isRequestedSessionIdFromUrl() {
		return delegate.isRequestedSessionIdFromUrl();
	}

	public boolean authenticate(HttpServletResponse response)
			throws IOException, ServletException {
		return delegate.authenticate(response);
	}

	public AsyncContext startAsync(ServletRequest servletRequest,
			ServletResponse servletResponse) throws IllegalStateException {
		return delegate.startAsync(servletRequest, servletResponse);
	}

	public void login(String username, String password) throws ServletException {
		delegate.login(username, password);
	}

	public void logout() throws ServletException {
		delegate.logout();
	}

	public Collection<Part> getParts() throws IOException, ServletException {
		return delegate.getParts();
	}

	public boolean isAsyncStarted() {
		return delegate.isAsyncStarted();
	}

	public boolean isAsyncSupported() {
		return delegate.isAsyncSupported();
	}

	public Part getPart(String name) throws IOException, ServletException {
		return delegate.getPart(name);
	}

	public AsyncContext getAsyncContext() {
		return delegate.getAsyncContext();
	}

	public DispatcherType getDispatcherType() {
		return delegate.getDispatcherType();
	}

	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
			throws IOException, ServletException {
		return delegate.upgrade(handlerClass);
	}
	
	
}
