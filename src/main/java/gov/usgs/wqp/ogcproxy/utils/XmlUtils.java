package gov.usgs.wqp.ogcproxy.utils;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.Document;

public class XmlUtils {

	//method to convert Document to String
	public static String domToString(Document doc) {
		String text = null;
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			text = writer.toString();
		} catch(TransformerException ex) {
			// returns null
		}
		return text;
	} 

}