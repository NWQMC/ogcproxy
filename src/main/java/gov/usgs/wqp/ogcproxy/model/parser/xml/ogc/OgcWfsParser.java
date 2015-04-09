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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class OgcWfsParser {
	
	private static final Pattern SERVICE = Pattern.compile("(service)=\"(\\S+)\"");
	private static final Pattern VERSION = Pattern.compile("(version)=\"([\\S]+)\"");
	private static final Pattern TYPE_NAME = Pattern.compile("(typeNames?)=\"(\\S+)\"");
	
	private static final Pattern[] OGC_PARAMS = new Pattern[]{VERSION, SERVICE, TYPE_NAME};

//	public void parse(HttpServletRequest request, Map<String, String> ogcParams, Map<String, String> searchParams) 
//			throws OGCProxyException {
//		try {
//			ogcParse(request, ogcParams);
//			String queryString = searchParse(request);
//		} catch (Exception e) {
//			throw new OGCProxyException(OGCProxyExceptionID.ERROR_READING_CLIENT_REQUEST_BODY, 
//					OgcWfsParser.class.getName(), "parse", "OGC parse error");
//		}
//		
//	}

	public Map<String,String> requestParamsPayloadToMap(HttpServletRequest request) {
		Map<String,String> params = new HashMap<String, String>();
		String queryString = "";
		try {
			queryString = searchParse(request);
		} catch (Exception e) {
			return params;
		}
		for (String param : queryString.split(";")) {
			String[] parts = param.split(":");
			if (parts.length != 2) {
				continue;
			}
			parts[1] = parts[1].replaceAll("%3A", ":").replaceAll("%7C", ";");
			params.put(parts[0], parts[1]);
		}
		
		return params;
	}
	
	String searchParse(HttpServletRequest request) throws Exception {
		String xml = contentToString(request);
		Document doc = document(xml);
//		NodeList filter = nodes(doc, "ogc:Filter");
		NodeList nodes = nodes(doc, "ogc:PropertyIsEqualTo");
		
		return findSearchParams(nodes);
	}

	
	String findSearchParams(NodeList nodes) {
		String params = null;
		Map<String,String> searchParams = new HashMap<String, String>();
		for (int j = 0; j < nodes.getLength(); j++) {
	        Node node = nodes.item(j);

	        textForMatchingTag(node, searchParams, "PropertyName");
        	textForMatchingTag(node, searchParams, "Literal");
	        
        	// did we find the searchParams
        	if (searchParams.containsKey("Literal") && "searchParams".equalsIgnoreCase( searchParams.get("PropertyName") )) {
	        	params = searchParams.get("Literal");
	        } else {
	        	params = findSearchParams(node.getChildNodes());
	        }
        	if (params != null) {
        		return params; // short circuit
        	}
		}
		return params;
	}

	void textForMatchingTag(Node child, Map<String, String> searchParams, String tagName) {
		if (child != null) {
			if (child.getNodeName().contains(tagName)) {
				searchParams.put(tagName, child.getTextContent());
			}
		}
	}

	public void ogcParse(HttpServletRequest request, Map<String, String> ogcParams) {
		String line;
		try {
			line = contentToString(request);
		} catch (Exception e) {
			return; // TODO maybe there should be some notification that this was unsuccessful?
		}
		
		for (Pattern param: OGC_PARAMS) {
			Matcher match = param.matcher(line);
			if (match.find()) {
				ogcParams.put(match.group(1), match.group(2));
			}
		}
		// TODO enh with typeName and typeNames version checking
	}
	
	
	String contentToString(HttpServletRequest request) throws Exception {
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
		line = line.trim();
//		line = line.replaceAll("\\s+<", "<");
//		line = line.replaceAll("\\s+", " ");
		
		return line;
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

	NodeList nodes(Document doc, String subtag) {
		NodeList nodes = doc.getElementsByTagName(subtag);
		
		return nodes;
	}
}
