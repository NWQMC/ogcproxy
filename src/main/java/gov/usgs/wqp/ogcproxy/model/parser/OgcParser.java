package gov.usgs.wqp.ogcproxy.model.parser;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyException;
import gov.usgs.wqp.ogcproxy.exceptions.OGCProxyExceptionID;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WFSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.parameters.WMSParameters;
import gov.usgs.wqp.ogcproxy.model.ogc.services.OGCServices;
import gov.usgs.wqp.ogcproxy.model.parameters.WQPParameters;

public class OgcParser {
	private static final Logger LOG = LoggerFactory.getLogger(OgcParser.class);

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

	public void parse() throws OGCProxyException {
		try {
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
		} catch (Exception e) {
			String msg = "Error parsing request body [" + e.getMessage() + "].";
			LOG.error(msg, e);

			OGCProxyExceptionID id = OGCProxyExceptionID.ERROR_READING_CLIENT_REQUEST_BODY;
			throw new OGCProxyException(id, "OgcParser", "parse()", msg);
		}
	}

	protected String getAttributeText(NamedNodeMap attrs, String attrName) {
		if (null != attrs && null != attrs.getNamedItem(attrName)) {
			return attrs.getNamedItem(attrName).getTextContent();
		} else {
			return "";
		}
	}

	protected void parseWfs(Document doc) throws UnsupportedEncodingException {
		NodeList nodes = doc.getElementsByTagNameNS("*", "Query");
		if (0 < nodes.getLength()) {
			//Expect one and only one Query node

			String[] typeName = new String[] {getAttributeText(nodes.item(0).getAttributes(), WFSParameters.typeName.toString())};
			String[] typeNames = new String[] {getAttributeText(nodes.item(0).getAttributes(), WFSParameters.typeNames.toString())};
			if (typeName.length > 0 && !typeName[0].isEmpty()) {
				requestParams.put(WFSParameters.typeName.toString(), typeName);
			}
			if (typeNames.length > 0 && !typeNames[0].isEmpty()) {
				requestParams.put(WFSParameters.typeNames.toString(), typeNames);
			}
	
			getVendorParams(doc.getElementsByTagNameNS("*", "PropertyIsEqualTo"));
		}
	}

	protected void parseWms(Document doc) throws UnsupportedEncodingException {
		NodeList nodes = doc.getElementsByTagNameNS("*", "NamedLayer");
		if (0 < nodes.getLength()) {
			//Expect one and only one NamedLayer node
			requestParams.put(WMSParameters.layers.toString(),
					new String[] {getNodeText((Element) nodes.item(0), "Name")});
	
			getVendorParams(doc.getElementsByTagNameNS("*", "Vendor"));
		}
	}

	protected void getVendorParams(NodeList vendorParams) throws UnsupportedEncodingException {
		for (int i = 0; i < vendorParams.getLength(); i++) {
			Node param = vendorParams.item(i);

			if (Node.ELEMENT_NODE == param.getNodeType() && isSearchParams((Element) param)) {
				requestParams.put(WQPParameters.searchParams.toString(), new String[] {getSearchParams((Element) param)});
				//Geoserver might not like our vendor param in the xml document, so just remove it all the time.
				param.getParentNode().removeChild(param);

				break;
			}
		}
	}

	protected boolean isSearchParams(Element element) {
		return WQPParameters.searchParams.toString().equalsIgnoreCase(getNodeText(element, "PropertyName"));
	}

	protected String getSearchParams(Element element) throws UnsupportedEncodingException {
		return UriUtils.decode(getNodeText(element, "Literal"), "UTF-8");
	}

	protected String getNodeText(Element element, String nodeName) {
		if (null != element && null != element.getElementsByTagNameNS("*", nodeName) &&
				0 < element.getElementsByTagNameNS("*", nodeName).getLength()) {
			//Only expect one.
			return element.getElementsByTagNameNS("*", nodeName).item(0).getTextContent();
		} else {
			return "";
		}
	}

	protected Document getDocument() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(request.getInputStream());
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
			LOG.trace("Could not convert Dcoument back to a String", ex);
		}
		return text;
	}

}
