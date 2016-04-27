package edu.ucsd.ccms.flow.test;

import org.w3c.dom.Document;

import edu.ucsd.saint.commons.xml.XmlUtils;

public class TestXML {


	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		XmlUtil util = XmlUtil.getInstance();
		Document doc = XmlUtils.createXML();
		doc.appendChild(doc.createElement("ROOT"));
		XmlUtils.printXML(doc, System.out);

		Document xml = XmlUtils.parseXML("data/cdcatalog.xml");
		XmlUtils.printXML(xml, System.out);

		Document xsl = XmlUtils.parseXML("data/cdcatalog.xsl");
		XmlUtils.transform(xsl, xml, System.out);

		try{
/*			NodeList nodes = (NodeList)xp.evaluate("*", src, XPathConstants.NODESET);
			for(int i = 0; i < nodes.getLength(); i++)
				printXML(nodes.item(i), System.out);*/

		}catch(Exception e){e.printStackTrace();};
	}
}
