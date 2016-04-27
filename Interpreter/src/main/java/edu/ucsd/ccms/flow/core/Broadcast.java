/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 */
package edu.ucsd.ccms.flow.core;


import java.net.URL;
import java.util.List;


import org.apache.log4j.xml.DOMConfigurator;
import org.w3c.dom.Node;

import dapper.codelet.Codelet;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.InputHandleResource;
import dapper.codelet.OutputHandleResource;
import dapper.codelet.Resource;

/**
 * A {@link Codelet} for broadcasting data from in-{@link Resource}s to all corresponding out-{@link Resource}s sharing
 * the same name.
 * 
 * @author Roy Liu
 */
public class Broadcast implements Codelet {

	static{
		URL log4j = ActionExec.class.getClassLoader().getResource("log4j.xml");
		if(log4j != null)
			DOMConfigurator.configure(log4j);
	}

	public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) throws Exception {
//		List<InputHandleResource> input = CodeletUtilities.filter(inResources, InputHandleResource.class);
//		List<OutputHandleResource> output = CodeletUtilities.filter(outResources, OutputHandleResource.class);
//		action = new Action(input, output, (Element)parameters);
//		action.printDebugInfo(input, output);

		for(Resource in: inResources){
			InputHandleResource ihr = (InputHandleResource)in;
			for (OutputHandleResource ohr :
				CodeletUtilities.filter(outResources, ihr.getName(), OutputHandleResource.class)) {
					ohr.put(ihr.get());
			}
		}

//		Map<String, List<Resource>> inResourcesMap = CodeletUtilities.groupByName(inResources);
//		for (Entry<String, List<Resource>> entry : inResourcesMap.entrySet()) {
//			List<Resource> resources = entry.getValue();
////			Control.checkTrue(resources.size() == 1, //
////					"In-resource list derived from the given name must be a singleton");
//			Resource resource = resources.get(0);
//			InputHandleResource ihr = (InputHandleResource) resource;
//			for (OutputHandleResource ohr :
//				CodeletUtilities.filter(outResources, ihr.getName(), OutputHandleResource.class)) {
//				ohr.put(ihr.get());
//			}
//		}
	}


	/**
	 * Default constructor.
	 */
	public Broadcast() {
	}

	/**
	 * Gets a human-readable description.
	 */
	@Override
	public String toString() {
		return "Broadcast";
	}
}
