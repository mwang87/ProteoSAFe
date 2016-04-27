package edu.ucsd.ccms.flow.action;


import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Element;

import edu.ucsd.saint.commons.xml.Wrapper;

public class TestMergeElement {

	@Test
	public void testMergeElements() {
		Wrapper d = new Wrapper();
		Element x = d.E("X",
			d.A("name", "this is X"), d.A("attr", "####"), 
			d.E("child", "child of X"));
		Element y = d.E("Y", 
			d.A("name", "this is Y"), d.A("value", "@@@@"), 
			d.E("child", "child of Y"));

		Element z = Action.mergeElements("merged", "test_id", x, y);
		assertEquals(z.getTagName(), "merged");
		assertEquals(z.getAttribute("id"), "test_id");
		assertEquals(z.getAttribute("name"), "this is Y");
		assertEquals(z.getAttribute("attr"), "####");
		assertEquals(z.getAttribute("value"), "@@@@");
		assertEquals(z.getElementsByTagName("child").getLength(), 2);
	}

}
