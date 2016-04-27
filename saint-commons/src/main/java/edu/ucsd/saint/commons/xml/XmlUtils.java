package edu.ucsd.saint.commons.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

public class XmlUtils
{
	private static Logger logger = LoggerFactory.getLogger(XmlUtils.class);
	private static TransformerFactory     xformFactory;
	private static DocumentBuilderFactory docFactory;
	
	static {
		try {
			xformFactory = TransformerFactory.newInstance();
			docFactory = DocumentBuilderFactory.newInstance();
		} catch (Throwable error) {
			logger.error("Failed to initialize XmlUtils", error);
		}
	}

	public static Document createXML() {
		// get document builder
		DocumentBuilder builder = null;
		try {
			builder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException error) {
			throw new RuntimeException(
				"Error instantiating XML DocumentBuilder", error);
		}
		return builder.newDocument();
	}

	public static Document parseXML(String xmlPath) {
		try {
			return parseXML(new FileInputStream(xmlPath));
		} catch(IOException error) {
			logger.info("Failed to parse XML file: " + xmlPath, error);
		}
		return null;
	}

	public static Document parseXML(InputStream stream) {
		// get document builder
		DocumentBuilder builder = null;
		try {
			builder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException error) {
			throw new RuntimeException(
				"Error instantiating XML DocumentBuilder", error);
		}
		// parse XML string into document
		Document document = null;
		try {
			document = builder.parse(stream);
		} catch (Throwable error) {
			throw new RuntimeException("Error parsing XML document", error);
		}
		return document;
	}

	public static void transform(Node xsl, Node xml, File path){
		try{
			OutputStream out = new FileOutputStream(path);
			transform(xsl, xml, out);
		}
		catch(Exception e){
			logger.info("Failed to transform XML file: " + path, e);
		}
	}

	public static void transform(Node xsl, Node xml, OutputStream out){
		try{
			Source stylesheet   = new DOMSource(xsl);
			Transformer xform   = xformFactory.newTransformer(stylesheet);
			Source original     = new DOMSource(xml);
			Result transformed  = new StreamResult(out);
			xform.transform(original, transformed);
		}
		catch(Exception e){
			logger.info("Failed to transform XML node", e);
		}
	}

	public static void printXML(Node xml, OutputStream out) {
		try {
		    Transformer transformer = xformFactory.newTransformer();
		    transformer.setOutputProperty(
		    	OutputKeys.OMIT_XML_DECLARATION, "yes");
		    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.transform(new DOMSource(xml), new StreamResult(out));
		} catch (Throwable error) {
			throw new RuntimeException("Error printing XML node", error);
		}
	}

	public static void prettyPrint(Node xml, OutputStream out){
		Document xsl = parseXML(XmlUtils.class.getResourceAsStream("/PrettyPrint.xslt"));
		transform(xsl, xml, out);
	}

	static{
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
	}

	public static Element getFirstElement(Document doc){
		return getFirstElement(doc.getDocumentElement());
	}

	public static Element getFirstElement(Element emt){
		Node node = emt.getFirstChild();
		while(node != null && node.getNodeType() != Node.ELEMENT_NODE)
			node = node.getNextSibling();
		return (Element) node;
	}

	public static Element getElement(Document doc, String ... tags){
		List<Element> list = getElements(doc, tags);
		return list.isEmpty() ? null : list.get(0);
	}

	public static Element getElement(Element emt, String ... tags){
		List<Element> list = getElements(emt, tags);
		return list.isEmpty() ? null : list.get(0);
	}

	public static List<Element> getElements(Document doc, String ... tags){
		return getElements(doc.getDocumentElement(), tags);
	}

	public static List<Element> getElements(Element emt, String ... tags){
		List<Element> result = new LinkedList<Element>();
		for(String tag: tags){
			NodeList nodes = emt.getElementsByTagName(tag);
			for(int i = 0; i < nodes.getLength(); i++)
				result.add((Element)nodes.item(i));
		}
		return result;
	}

	public static List<Element> getChildElements(Element emt){
		NodeList nodes = emt.getChildNodes();
		List<Element> result = new LinkedList<Element>();
		for(int i = 0; i < nodes.getLength(); i++){
			Node n = nodes.item(i);
			if(n.getNodeType() == Node.ELEMENT_NODE)
				result.add((Element)nodes.item(i));
		}
		return result;
	}

	public static Map<String, Element> getNamedMap(Document root, final String tag){
		return getNamedMap(root.getDocumentElement(), tag);
	}

	public static Map<String, Element> getNamedMap(Element root, final String tag){
		return getNamedMap(root, tag, "name");
	}

	public static Map<String, Element> getNamedMap(Element root, final String tag, final String attr){
		Map<String, Element>map = new HashMap<String, Element>();
		for(Element emt: getElements(root, tag)){
			String name = emt.getAttribute(attr);
			if(name != null) map.put(name, emt);
		}
		return map;
	}
}
