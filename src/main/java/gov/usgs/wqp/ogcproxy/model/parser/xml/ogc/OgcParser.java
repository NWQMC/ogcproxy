package gov.usgs.wqp.ogcproxy.model.parser.xml.ogc;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.web.util.UriUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;

public class OgcParser {

	private final HttpServletRequest request;
	private String bodyMinusVendorParams;
	private Map<String, String[]> requestParams;

	public OgcParser(HttpServletRequest request) {
		this.request = request;
		this.requestParams = new HashMap<>();
	}
	
	public Map<String, String[]> getRequestParamsAsMap() {
		return requestParams;
	}

	public String getBodyMinusVendorParams() {
		return bodyMinusVendorParams;
	}

	public void parse() throws Exception {
		Document doc = getDocument();
		Node root = doc.getDocumentElement();
		
		requestParams.put("request", new String[] {root.getLocalName()});
		requestParams.put("version", new String[] {getAttributeText(root.getAttributes(), "version")});
		String service = getAttributeText(root.getAttributes(), "service");
		requestParams.put("service", new String[] {service});
		
		if (OGCServices.WFS.toString().contentEquals(service)) {
			parseWfs(doc);
		} else {
			parseWms(doc);
		}
		
		bodyMinusVendorParams = domToString(doc);
	}

	protected String getAttributeText(NamedNodeMap attrs, String attrName) {
		if (null != attrs && null != attrs.getNamedItem(attrName)) {
			return attrs.getNamedItem(attrName).getTextContent();
		} else {
			return "";
		}
	}

	protected void parseWfs(Document doc) throws UnsupportedEncodingException {
		NodeList nodes = doc.getElementsByTagName("wfs:Query");
		//Only expect one Query node
		requestParams.put(WFSParameters.typeName.toString(),
				new String[] {getAttributeText(nodes.item(0).getAttributes(), WFSParameters.typeName.toString())});
		requestParams.put(WFSParameters.typeNames.toString(),
				new String[] {getAttributeText(nodes.item(0).getAttributes(), WFSParameters.typeNames.toString())});
		
		getVendorParams(doc.getElementsByTagName("ogc:PropertyIsEqualTo"));
	}

	protected void parseWms(Document doc) throws UnsupportedEncodingException {
		NodeList nodes = doc.getElementsByTagName("NamedLayer");
		//Only expect one NamedLayer node
		requestParams.put(WMSParameters.layers.toString(),
				new String[] {getNodeText((Element) nodes.item(0), "Name")});
		
		getVendorParams(doc.getElementsByTagName("ogc:PropertyIsEqualTo"));
	}

	protected void getVendorParams(NodeList vendorParams) throws UnsupportedEncodingException {
		for (int i = 0; i < vendorParams.getLength(); i++) {
			Node param = vendorParams.item(i);
		
			if (Node.ELEMENT_NODE == param.getNodeType()) {
				if (isSearchParams((Element) param)) {
					requestParams.put(WQPParameters.searchParams.toString(), new String[] {getSearchParams((Element) param)});
					//Geoserver might not like our vendor param in the xml document, so just remove it all the time.
					param.getParentNode().removeChild(param);
					
					break;
				}
			}
		}
	}

	protected boolean isSearchParams(Element element) {
		return WQPParameters.searchParams.toString().equalsIgnoreCase(getNodeText(element, "ogc:PropertyName"));
	}
	
	protected String getSearchParams(Element element) throws UnsupportedEncodingException {
		return UriUtils.decode(getNodeText(element, "ogc:Literal"), "UTF-8");
	}

	protected String getNodeText(Element element, String nodeName) {
		if (null != element && null != element.getElementsByTagName(nodeName) &&
				0 < element.getElementsByTagName(nodeName).getLength()) {
			//Only expect one.
			return element.getElementsByTagName(nodeName).item(0).getTextContent();
		} else {
			return "";
		}
	}

	protected Document getDocument() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(request.getInputStream());
		return doc;
	}

	protected String domToString(Document doc) {
		String text = null;
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			text = writer.toString();
		} catch (TransformerException ex) {
			// returns null
		}
		return text;
	}

}
