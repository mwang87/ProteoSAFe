package edu.ucsd.ccms.flow.test;


import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class TestAppendElement {

	public static void main(String[] args) {
//		XmlUtil util = XmlUtil.getInstance();

		Document xml = XmlUtils.parseXML("a.xml");
		Element doc = xml.getDocumentElement();
		List<Element> inputs = XmlUtils.getElements(doc, "input");
		for(Element i: inputs)
			doc.appendChild(i.cloneNode(true));
		XmlUtils.getElements(doc, "");

		XmlUtils.printXML(doc, System.out);
		XmlUtils.printXML(xml, System.out);

		try{
		}catch(Exception e){e.printStackTrace();};
	}
}
