function ProteoSAFeInputModule_staticForward(div, id, properties) {
	this.div = div;
	this.id = id;
	// no real loading needs to take place for this form element
	ProteoSAFeInputUtils.setAsynchronousElementLoaded(this.div, this.id);
	// find the "location" property and forward to it
	if (properties != null) {
		var location = properties.location;
		if (location != null)
			window.location = location;
	}
}

CCMSForms.modules["staticForward"] = ProteoSAFeInputModule_staticForward;
