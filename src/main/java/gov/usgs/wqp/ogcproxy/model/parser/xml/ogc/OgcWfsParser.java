package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.web.util.UriUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.usgs.wqp.ogcproxy.utils.XmlUtils;


public class OgcWfsParser {
	
	private static final Pattern SERVICE = Pattern.compile("(service)=\"(\\S+)\"");
	private static final Pattern VERSION = Pattern.compile("(version)=\"([\\S]+)\"");
	private static final Pattern TYPE_NAME = Pattern.compile("(typeNames?)=\"(\\S+)\"");
	
	private static final Pattern[] OGC_PARAMS = new Pattern[]{VERSION, SERVICE, TYPE_NAME};

	private final HttpServletRequest request;
	private String body;
	private String bodyMinusSearchParams;
	
	
	public OgcWfsParser(HttpServletRequest request) {
		this.request = request;
	}
	
	
	public Map<String,String> requestParamsPayloadToMap() {
		Map<String,String> params = ogcParse();
		String queryString = "";
		try {
			queryString = searchParams();
			params.put("searchParams", queryString);
		} catch (Exception e) {
			return params;
		}
		
		return params;
	}
	
	String searchParams() throws Exception {
		String xml     = getBody();
		Document doc   = document(xml);
		NodeList nodes = nodes(doc.getDocumentElement(), "ogc:PropertyIsEqualTo");
		Node node      = findSearchParamsNode(nodes);
		
		if (node == null) {
			// TODO this is a side effect and should be done elsewhere however we have the node now
			bodyMinusSearchParams = body;
			return "";
		}
		String params  = extractSearchParams(node);
		
		// TODO this is a side effect and should be done elsewhere however we have the node now
		node.getParentNode().removeChild(node);
		bodyMinusSearchParams = XmlUtils.domToString(doc);
		
		if (params!=null) {
			params = UriUtils.decode(params, "UTF-8");
		}
		return params;
	}
	
	Node findSearchParamsNode(NodeList nodes) {
		boolean isSearchParams = false;
		for (int j = 0; j < nodes.getLength(); j++) {
	        Node node = nodes.item(j);

	        String propertyName   = textForMatchingTag(node, "PropertyName");
	        isSearchParams = "searchParams".equalsIgnoreCase(propertyName);
	        
	    	// did we find the searchParam node?
	    	if (isSearchParams) {
	    		return node.getParentNode();
	        }
	    	
	    	// check all children
	    	node = findSearchParamsNode(node.getChildNodes());
	    	
	    	// short circuit: stop looping if we found the node
        	if (node != null) {
	    		return node;
        	}
		}
		// if we have not found it then return nothing
		return null;
	}
	
	
	String extractSearchParams(Node node) {
		Node propertyNode   = node.getFirstChild().getNextSibling();
        String propertyName = textForMatchingTag(propertyNode, "PropertyName");
        
    	// did we find the searchParams
    	if ( "searchParams".equalsIgnoreCase(propertyName) ) {
        	String searchParams = textForMatchingTag(node.getLastChild().getPreviousSibling(), "Literal");
        	if (searchParams.toLowerCase().startsWith("searchparams=")) {
        		searchParams = searchParams.substring(13);
        	}
        	return searchParams;
        }
		return "";
	}

	String textForMatchingTag(Node child, String tagName) {
		if (child != null && child.getNodeName().contains(tagName)) {
			return child.getTextContent();
		}
		return null;
	}

	public Map<String, String> ogcParse() {
		Map<String, String> ogcParams = new HashMap<String, String>();
		
		String body;
		try {
			body = getBody();
		} catch (Exception e) {
			return ogcParams;
			// TODO maybe there should be some notification that this was unsuccessful?
		}
		
		for (Pattern param: OGC_PARAMS) {
			Matcher match = param.matcher(body);
			if (match.find()) {
				ogcParams.put(match.group(1), match.group(2));
			}
		}
		
		return ogcParams;
		// TODO enh with typeName and typeNames version checking
	}
	
	public String getBodyMinusSearchParams() {
		return bodyMinusSearchParams;
	}
	String getBody() throws Exception {
		if (body != null) {
			return body;
		}
		
		Reader reader = new InputStreamReader(request.getInputStream());
		BufferedReader buffer = new BufferedReader(reader);
		
		StringBuilder builder = new StringBuilder();
		
		String line = null;
		while ( (line=buffer.readLine()) != null) {
			builder.append(line);
		}
		
		int index = builder.indexOf("?>");
		
		if (index >= 0) {
			line = builder.substring(index+2);
		} else {
			line = builder.toString();
		}
		body = line.trim().replaceAll("><", "> <");
		
		return body;
	}
	
	
	Document document(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
//	    dbf.setValidating(false);
//	    dbf.setIgnoringComments(true);
//	    dbf.setIgnoringElementContentWhitespace(true);
//	    dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse( new ByteArrayInputStream(xml.getBytes()) );
		return doc;
	}

	NodeList nodes(Element node, String subtag) {
		NodeList nodes = node.getElementsByTagName(subtag);
		
		return nodes;
	}
}
