package edu.ucsd.saint.commons.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Wrapper {

	private Document doc;

	public Wrapper (){
		this(XmlUtils.createXML());
	}

	public Wrapper (Document doc){
		this.doc = doc;
	}

	public Wrapper (Node node){
		this.doc = node.getOwnerDocument();
	}

	public Element E(String tag, Node... nodes){
		Element e = doc.createElement(tag);
		for(Node n: nodes){
			if(n.getParentNode() != null)
				n = n.cloneNode(true);
			if(n.getOwnerDocument() != doc)
				n = doc.importNode(n, true);
			
			switch(n.getNodeType()){
			case Node.ATTRIBUTE_NODE:
				e.setAttributeNode((Attr)n); continue;
			case Node.ELEMENT_NODE:
			case Node.TEXT_NODE:
				e.appendChild(n); continue;
			}
		}
		return e;
	}

	public Element E(String tag, String text){
		Element e = doc.createElement(tag);
		e.appendChild(T(text));
		return e;
	}

	public Node A(String name, String value){
		Attr attr = doc.createAttribute(name);
		attr.setNodeValue(value);
		return attr;
	}

	public Node T(String val){
		return doc.createTextNode(val);
	}
}
