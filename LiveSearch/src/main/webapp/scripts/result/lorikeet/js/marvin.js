//
// Inserted the content of http://java.com/js/deployJava.js
//

var deployJava=function(){var l={core:["id","class","title","style"],i18n:["lang","dir"],events:["onclick","ondblclick","onmousedown","onmouseup","onmouseover","onmousemove","onmouseout","onkeypress","onkeydown","onkeyup"],applet:["codebase","code","name","archive","object","width","height","alt","align","hspace","vspace"],object:["classid","codebase","codetype","data","type","archive","declare","standby","height","width","usemap","name","tabindex","align","border","hspace","vspace"]};var b=l.object.concat(l.core,l.i18n,l.events);var m=l.applet.concat(l.core);function g(o){if(!d.debug){return}if(console.log){console.log(o)}else{alert(o)}}function k(p,o){if(p==null||p.length==0){return true}var r=p.charAt(p.length-1);if(r!="+"&&r!="*"&&(p.indexOf("_")!=-1&&r!="_")){p=p+"*";r="*"}p=p.substring(0,p.length-1);if(p.length>0){var q=p.charAt(p.length-1);if(q=="."||q=="_"){p=p.substring(0,p.length-1)}}if(r=="*"){return(o.indexOf(p)==0)}else{if(r=="+"){return p<=o}}return false}function e(){var o="//java.com/js/webstart.png";try{return document.location.protocol.indexOf("http")!=-1?o:"http:"+o}catch(p){return"http:"+o}}function n(p){var o="http://java.com/dt-redirect";if(p==null||p.length==0){return o}if(p.charAt(0)=="&"){p=p.substring(1,p.length)}return o+"?"+p}function j(q,p){var o=q.length;for(var r=0;r<o;r++){if(q[r]===p){return true}}return false}function c(o){return j(m,o.toLowerCase())}function i(o){return j(b,o.toLowerCase())}function a(o){if("MSIE"!=deployJava.browserName){return true}if(deployJava.compareVersionToPattern(deployJava.getPlugin().version,["10","0","0"],false,true)){return true}if(o==null){return false}return !k("1.6.0_33+",o)}var d={debug:null,version:"20120801",firefoxJavaVersion:null,myInterval:null,preInstallJREList:null,returnPage:null,brand:null,locale:null,installType:null,EAInstallEnabled:false,EarlyAccessURL:null,oldMimeType:"application/npruntime-scriptable-plugin;DeploymentToolkit",mimeType:"application/java-deployment-toolkit",launchButtonPNG:e(),browserName:null,browserName2:null,getJREs:function(){var t=new Array();if(this.isPluginInstalled()){var r=this.getPlugin();var o=r.jvms;for(var q=0;q<o.getLength();q++){t[q]=o.get(q).version}}else{var p=this.getBrowser();if(p=="MSIE"){if(this.testUsingActiveX("1.7.0")){t[0]="1.7.0"}else{if(this.testUsingActiveX("1.6.0")){t[0]="1.6.0"}else{if(this.testUsingActiveX("1.5.0")){t[0]="1.5.0"}else{if(this.testUsingActiveX("1.4.2")){t[0]="1.4.2"}else{if(this.testForMSVM()){t[0]="1.1"}}}}}}else{if(p=="Netscape Family"){this.getJPIVersionUsingMimeType();if(this.firefoxJavaVersion!=null){t[0]=this.firefoxJavaVersion}else{if(this.testUsingMimeTypes("1.7")){t[0]="1.7.0"}else{if(this.testUsingMimeTypes("1.6")){t[0]="1.6.0"}else{if(this.testUsingMimeTypes("1.5")){t[0]="1.5.0"}else{if(this.testUsingMimeTypes("1.4.2")){t[0]="1.4.2"}else{if(this.browserName2=="Safari"){if(this.testUsingPluginsArray("1.7.0")){t[0]="1.7.0"}else{if(this.testUsingPluginsArray("1.6")){t[0]="1.6.0"}else{if(this.testUsingPluginsArray("1.5")){t[0]="1.5.0"}else{if(this.testUsingPluginsArray("1.4.2")){t[0]="1.4.2"}}}}}}}}}}}}}if(this.debug){for(var q=0;q<t.length;++q){g("[getJREs()] We claim to have detected Java SE "+t[q])}}return t},installJRE:function(r,p){var o=false;if(this.isPluginInstalled()&&this.isAutoInstallEnabled(r)){var q=false;if(this.isCallbackSupported()){q=this.getPlugin().installJRE(r,p)}else{q=this.getPlugin().installJRE(r)}if(q){this.refresh();if(this.returnPage!=null){document.location=this.returnPage}}return q}else{return this.installLatestJRE()}},isAutoInstallEnabled:function(o){if(!this.isPluginInstalled()){return false}if(typeof o=="undefined"){o=null}return a(o)},isCallbackSupported:function(){return this.isPluginInstalled()&&this.compareVersionToPattern(this.getPlugin().version,["10","2","0"],false,true)},installLatestJRE:function(q){if(this.isPluginInstalled()&&this.isAutoInstallEnabled()){var r=false;if(this.isCallbackSupported()){r=this.getPlugin().installLatestJRE(q)}else{r=this.getPlugin().installLatestJRE()}if(r){this.refresh();if(this.returnPage!=null){document.location=this.returnPage}}return r}else{var p=this.getBrowser();var o=navigator.platform.toLowerCase();if((this.EAInstallEnabled=="true")&&(o.indexOf("win")!=-1)&&(this.EarlyAccessURL!=null)){this.preInstallJREList=this.getJREs();if(this.returnPage!=null){this.myInterval=setInterval("deployJava.poll()",3000)}location.href=this.EarlyAccessURL;return false}else{if(p=="MSIE"){return this.IEInstall()}else{if((p=="Netscape Family")&&(o.indexOf("win32")!=-1)){return this.FFInstall()}else{location.href=n(((this.returnPage!=null)?("&returnPage="+this.returnPage):"")+((this.locale!=null)?("&locale="+this.locale):"")+((this.brand!=null)?("&brand="+this.brand):""))}}return false}}},runApplet:function(p,u,r){if(r=="undefined"||r==null){r="1.1"}var t="^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";var o=r.match(t);if(this.returnPage==null){this.returnPage=document.location}if(o!=null){var q=this.getBrowser();if(q!="?"){if(this.versionCheck(r+"+")){this.writeAppletTag(p,u)}else{if(this.installJRE(r+"+")){this.refresh();location.href=document.location;this.writeAppletTag(p,u)}}}else{this.writeAppletTag(p,u)}}else{g("[runApplet()] Invalid minimumVersion argument to runApplet():"+r)}},writeAppletTag:function(r,w){var o="<"+"applet ";var q="";var t="<"+"/"+"applet"+">";var x=true;if(null==w||typeof w!="object"){w=new Object()}for(var p in r){if(!c(p)){w[p]=r[p]}else{o+=(" "+p+'="'+r[p]+'"');if(p=="code"){x=false}}}var v=false;for(var u in w){if(u=="codebase_lookup"){v=true}if(u=="object"||u=="java_object"||u=="java_code"){x=false}q+='<param name="'+u+'" value="'+w[u]+'"/>'}if(!v){q+='<param name="codebase_lookup" value="false"/>'}if(x){o+=(' code="dummy"')}o+=">";document.write(o+"\n"+q+"\n"+t)},versionCheck:function(p){var v=0;var x="^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?(\\*|\\+)?$";var y=p.match(x);if(y!=null){var r=false;var u=false;var q=new Array();for(var t=1;t<y.length;++t){if((typeof y[t]=="string")&&(y[t]!="")){q[v]=y[t];v++}}if(q[q.length-1]=="+"){u=true;r=false;q.length--}else{if(q[q.length-1]=="*"){u=false;r=true;q.length--}else{if(q.length<4){u=false;r=true}}}var w=this.getJREs();for(var t=0;t<w.length;++t){if(this.compareVersionToPattern(w[t],q,r,u)){return true}}return false}else{var o="Invalid versionPattern passed to versionCheck: "+p;g("[versionCheck()] "+o);alert(o);return false}},isWebStartInstalled:function(r){var q=this.getBrowser();if(q=="?"){return true}if(r=="undefined"||r==null){r="1.4.2"}var p=false;var t="^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";var o=r.match(t);if(o!=null){p=this.versionCheck(r+"+")}else{g("[isWebStartInstaller()] Invalid minimumVersion argument to isWebStartInstalled(): "+r);p=this.versionCheck("1.4.2+")}return p},getJPIVersionUsingMimeType:function(){for(var p=0;p<navigator.mimeTypes.length;++p){var q=navigator.mimeTypes[p].type;var o=q.match(/^application\/x-java-applet;jpi-version=(.*)$/);if(o!=null){this.firefoxJavaVersion=o[1];if("Opera"!=this.browserName2){break}}}},launchWebStartApplication:function(r){var o=navigator.userAgent.toLowerCase();this.getJPIVersionUsingMimeType();if(this.isWebStartInstalled("1.7.0")==false){if((this.installJRE("1.7.0+")==false)||((this.isWebStartInstalled("1.7.0")==false))){return false}}var u=null;if(document.documentURI){u=document.documentURI}if(u==null){u=document.URL}var p=this.getBrowser();var q;if(p=="MSIE"){q="<"+'object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" '+'width="0" height="0">'+"<"+'PARAM name="launchjnlp" value="'+r+'"'+">"+"<"+'PARAM name="docbase" value="'+u+'"'+">"+"<"+"/"+"object"+">"}else{if(p=="Netscape Family"){q="<"+'embed type="application/x-java-applet;jpi-version='+this.firefoxJavaVersion+'" '+'width="0" height="0" '+'launchjnlp="'+r+'"'+'docbase="'+u+'"'+" />"}}if(document.body=="undefined"||document.body==null){document.write(q);document.location=u}else{var t=document.createElement("div");t.id="div1";t.style.position="relative";t.style.left="-10000px";t.style.margin="0px auto";t.className="dynamicDiv";t.innerHTML=q;document.body.appendChild(t)}},createWebStartLaunchButtonEx:function(q,p){if(this.returnPage==null){this.returnPage=q}var o="javascript:deployJava.launchWebStartApplication('"+q+"');";document.write("<"+'a href="'+o+"\" onMouseOver=\"window.status=''; "+'return true;"><'+"img "+'src="'+this.launchButtonPNG+'" '+'border="0" /><'+"/"+"a"+">")},createWebStartLaunchButton:function(q,p){if(this.returnPage==null){this.returnPage=q}var o="javascript:"+"if (!deployJava.isWebStartInstalled(&quot;"+p+"&quot;)) {"+"if (deployJava.installLatestJRE()) {"+"if (deployJava.launch(&quot;"+q+"&quot;)) {}"+"}"+"} else {"+"if (deployJava.launch(&quot;"+q+"&quot;)) {}"+"}";document.write("<"+'a href="'+o+"\" onMouseOver=\"window.status=''; "+'return true;"><'+"img "+'src="'+this.launchButtonPNG+'" '+'border="0" /><'+"/"+"a"+">")},launch:function(o){document.location=o;return true},isPluginInstalled:function(){var o=this.getPlugin();if(o&&o.jvms){return true}else{return false}},isAutoUpdateEnabled:function(){if(this.isPluginInstalled()){return this.getPlugin().isAutoUpdateEnabled()}return false},setAutoUpdateEnabled:function(){if(this.isPluginInstalled()){return this.getPlugin().setAutoUpdateEnabled()}return false},setInstallerType:function(o){this.installType=o;if(this.isPluginInstalled()){return this.getPlugin().setInstallerType(o)}return false},setAdditionalPackages:function(o){if(this.isPluginInstalled()){return this.getPlugin().setAdditionalPackages(o)}return false},setEarlyAccess:function(o){this.EAInstallEnabled=o},isPlugin2:function(){if(this.isPluginInstalled()){if(this.versionCheck("1.6.0_10+")){try{return this.getPlugin().isPlugin2()}catch(o){}}}return false},allowPlugin:function(){this.getBrowser();var o=("Safari"!=this.browserName2&&"Opera"!=this.browserName2);return o},getPlugin:function(){this.refresh();var o=null;if(this.allowPlugin()){o=document.getElementById("deployJavaPlugin")}return o},compareVersionToPattern:function(v,p,r,t){if(v==undefined||p==undefined){return false}var w="^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:_(\\d+))?)?)?$";var x=v.match(w);if(x!=null){var u=0;var y=new Array();for(var q=1;q<x.length;++q){if((typeof x[q]=="string")&&(x[q]!="")){y[u]=x[q];u++}}var o=Math.min(y.length,p.length);if(t){for(var q=0;q<o;++q){if(y[q]<p[q]){return false}else{if(y[q]>p[q]){return true}}}return true}else{for(var q=0;q<o;++q){if(y[q]!=p[q]){return false}}if(r){return true}else{return(y.length==p.length)}}}else{return false}},getBrowser:function(){if(this.browserName==null){var o=navigator.userAgent.toLowerCase();g("[getBrowser()] navigator.userAgent.toLowerCase() -> "+o);if((o.indexOf("msie")!=-1)&&(o.indexOf("opera")==-1)){this.browserName="MSIE";this.browserName2="MSIE"}else{if(o.indexOf("trident")!=-1||o.indexOf("Trident")!=-1){this.browserName="MSIE";this.browserName2="MSIE"}else{if(o.indexOf("iphone")!=-1){this.browserName="Netscape Family";this.browserName2="iPhone"}else{if((o.indexOf("firefox")!=-1)&&(o.indexOf("opera")==-1)){this.browserName="Netscape Family";this.browserName2="Firefox"}else{if(o.indexOf("chrome")!=-1){this.browserName="Netscape Family";this.browserName2="Chrome"}else{if(o.indexOf("safari")!=-1){this.browserName="Netscape Family";this.browserName2="Safari"}else{if((o.indexOf("mozilla")!=-1)&&(o.indexOf("opera")==-1)){this.browserName="Netscape Family";this.browserName2="Other"}else{if(o.indexOf("opera")!=-1){this.browserName="Netscape Family";this.browserName2="Opera"}else{this.browserName="?";this.browserName2="unknown"}}}}}}}}g("[getBrowser()] Detected browser name:"+this.browserName+", "+this.browserName2)}return this.browserName},testUsingActiveX:function(o){var q="JavaWebStart.isInstalled."+o+".0";if(typeof ActiveXObject=="undefined"||!ActiveXObject){g("[testUsingActiveX()] Browser claims to be IE, but no ActiveXObject object?");return false}try{return(new ActiveXObject(q)!=null)}catch(p){return false}},testForMSVM:function(){var p="{08B0E5C0-4FCB-11CF-AAA5-00401C608500}";if(typeof oClientCaps!="undefined"){var o=oClientCaps.getComponentVersion(p,"ComponentID");if((o=="")||(o=="5,0,5000,0")){return false}else{return true}}else{return false}},testUsingMimeTypes:function(p){if(!navigator.mimeTypes){g("[testUsingMimeTypes()] Browser claims to be Netscape family, but no mimeTypes[] array?");return false}for(var q=0;q<navigator.mimeTypes.length;++q){s=navigator.mimeTypes[q].type;var o=s.match(/^application\/x-java-applet\x3Bversion=(1\.8|1\.7|1\.6|1\.5|1\.4\.2)$/);if(o!=null){if(this.compareVersions(o[1],p)){return true}}}return false},testUsingPluginsArray:function(p){if((!navigator.plugins)||(!navigator.plugins.length)){return false}var o=navigator.platform.toLowerCase();for(var q=0;q<navigator.plugins.length;++q){s=navigator.plugins[q].description;if(s.search(/^Java Switchable Plug-in (Cocoa)/)!=-1){if(this.compareVersions("1.5.0",p)){return true}}else{if(s.search(/^Java/)!=-1){if(o.indexOf("win")!=-1){if(this.compareVersions("1.5.0",p)||this.compareVersions("1.6.0",p)){return true}}}}}if(this.compareVersions("1.5.0",p)){return true}return false},IEInstall:function(){location.href=n(((this.returnPage!=null)?("&returnPage="+this.returnPage):"")+((this.locale!=null)?("&locale="+this.locale):"")+((this.brand!=null)?("&brand="+this.brand):""));return false},done:function(p,o){},FFInstall:function(){location.href=n(((this.returnPage!=null)?("&returnPage="+this.returnPage):"")+((this.locale!=null)?("&locale="+this.locale):"")+((this.brand!=null)?("&brand="+this.brand):"")+((this.installType!=null)?("&type="+this.installType):""));return false},compareVersions:function(r,t){var p=r.split(".");var o=t.split(".");for(var q=0;q<p.length;++q){p[q]=Number(p[q])}for(var q=0;q<o.length;++q){o[q]=Number(o[q])}if(p.length==2){p[2]=0}if(p[0]>o[0]){return true}if(p[0]<o[0]){return false}if(p[1]>o[1]){return true}if(p[1]<o[1]){return false}if(p[2]>o[2]){return true}if(p[2]<o[2]){return false}return true},enableAlerts:function(){this.browserName=null;this.debug=true},poll:function(){this.refresh();var o=this.getJREs();if((this.preInstallJREList.length==0)&&(o.length!=0)){clearInterval(this.myInterval);if(this.returnPage!=null){location.href=this.returnPage}}if((this.preInstallJREList.length!=0)&&(o.length!=0)&&(this.preInstallJREList[0]!=o[0])){clearInterval(this.myInterval);if(this.returnPage!=null){location.href=this.returnPage}}},writePluginTag:function(){var o=this.getBrowser();if(o=="MSIE"){document.write("<"+'object classid="clsid:CAFEEFAC-DEC7-0000-0001-ABCDEFFEDCBA" '+'id="deployJavaPlugin" width="0" height="0">'+"<"+"/"+"object"+">")}else{if(o=="Netscape Family"&&this.allowPlugin()){this.writeEmbedTag()}}},refresh:function(){navigator.plugins.refresh(false);var o=this.getBrowser();if(o=="Netscape Family"&&this.allowPlugin()){var p=document.getElementById("deployJavaPlugin");if(p==null){this.writeEmbedTag()}}},writeEmbedTag:function(){var o=false;if(navigator.mimeTypes!=null){for(var p=0;p<navigator.mimeTypes.length;p++){if(navigator.mimeTypes[p].type==this.mimeType){if(navigator.mimeTypes[p].enabledPlugin){document.write("<"+'embed id="deployJavaPlugin" type="'+this.mimeType+'" hidden="true" />');o=true}}}if(!o){for(var p=0;p<navigator.mimeTypes.length;p++){if(navigator.mimeTypes[p].type==this.oldMimeType){if(navigator.mimeTypes[p].enabledPlugin){document.write("<"+'embed id="deployJavaPlugin" type="'+this.oldMimeType+'" hidden="true" />')}}}}}}};d.writePluginTag();if(d.locale==null){var h=null;if(h==null){try{h=navigator.userLanguage}catch(f){}}if(h==null){try{h=navigator.systemLanguage}catch(f){}}if(h==null){try{h=navigator.language}catch(f){}}if(h!=null){h.replace("-","_");d.locale=h}}return d}();

//
// -------------------------------------------------------------------------------
//

//////////////////////////////////////////////////////////////////////////
//// Marvin utility functions
//// Copyright (c) 1998-2015 ChemAxon Ltd., Peter Csizmadia,
////                          Ferenc Csizmadia, Tamas Vertse, Gabor Bartha
//////////////////////////////////////////////////////////////////////////

var modernjava = "1.6.0_10";
var minmarvinjava = "1.6"


// Check whether supported Java version is installed
// @minjver - minimum Java requirement
// @return true - if required Java is available, else false
function isJavaInstalled(minjver) {
	var jplugins = deployJava.getJREs(); 
	if(jplugins == "") { // no java plugins
		return false;
	}
	var status = false;
	// check only the last plugin
	for(count=0; count < jplugins.length; count++) {
		status = (jplugins[count] >= minjver);
	}
	return status;
}

// Check whether the current browser is Firefox 3.6.x
function isFirefox3_6() {
	var s = navigator.userAgent;
	if(s.lastIndexOf("Firefox/3.6") > -1) {
		return true;
	}
	return false;
}


// Check whether current browser supports installed Java.
// return 0 - Proper Java Plugin is installed, 1 - Firefox 3.6 without modern java, 2 - No Java Plugin.
function checkJava() {
	if(isFirefox3_6()) { // current browser is Firefox 3.6.x
		if(!isJavaInstalled(modernjava)) { // not modern java
			return 1;
		}
	} else {
		if(deployJava.getJREs().length == 0) { // no Java Plugin
			return 2;
		}
	}
	if(!isJavaInstalled(minmarvinjava)) { // Marvin's Java requirement
		return 3;
	}
	return 0;
}

// Provides error message for the given error code
// see error codes in checkJava function
function getJavaErrorMessage(errorcode) {
	var msg = "";
	if(errorcode == 1) {
		msg = "The applet cannot run, because Firefox 3.6 is only supported with \n"+
"next generation Java plug-in versions JRE 1.6.0_10 or higher.";
	} else if(errorcode == 2) {
		msg = "The applet cannot run because your java plug-in is not available.";
	} else if(errorcode == 3) {
		msg = "The version number of the Java plugin is lower than "+minmarvinjava+".\n"+
			"Current Marvin requires at least version "+minmarvinjava+".";
	}
	return msg;
}

// ----------------------------------------------------------------------------------------

//
// "Public" parameters that can be specified before msketch_begin/mview_begin.
//

// The MAYSCRIPT attribute for MarvinView.
var mview_mayscript = false;

// The MAYSCRIPT attribute for MarvinSketch.
var msketch_mayscript = false;

// Applet names, unspecified by default
var msketch_name = "";
var mview_name = "";

var msketch_legacy=!isLeopardSafari();
var mview_legacy=!isLeopardSafari();

var humanreadable=false;

// Applet can use these additional jar files. If more then one additional
// file are used, files has to be separated by colon. 
// e.g to generate svg from the applet, the batik-core.jar has to be used.
var msketch_usedJars = "";

// Use "builtin" for the browser's default JVM, "plugin" for the Java Plugin.
var marvin_jvm = "";

// GUI used: "awt" or "swing"
var marvin_gui = "";

//
// Internal functions
//

var marvin_jvm0 = "";
var marvin_gui0 = "";
var applet_type; // depends on marvin_jvm, 0=<applet>, 1=<embed>, 2=<object>

// displays an image on the applet's canvas while applet is loading
var loading_image = "img/loading.gif";

// Set marvin_jvm if the URL of the HTML page has a jvm parameter.
if(location.search.lastIndexOf("jvm=plugin") >= 0) {
	marvin_jvm0 = "plugin";
	if(browser_mozilla_version()==5) {//builtin if Netscape 6-
	    marvin_jvm0 = "builtin";
	}
}
if(location.search.lastIndexOf("jvm=builtin") >= 0) {
	marvin_jvm0 = "builtin";
}

// Set marvin_gui if the location string contains the gui parameter.
if(location.search.lastIndexOf("gui=swing") >= 0) {
	marvin_gui0 = "swing";
}
if(location.search.lastIndexOf("gui=awt") >= 0) {
	marvin_gui0 = "awt";
}

var _appletstrbuf = "";

function browser_parse_version0(name) {
	var brz = navigator.userAgent;
	var i = brz.lastIndexOf(name);
	if(i >= 0) {
		var s = brz.substring(i + name.length);
		var j = s.indexOf(".");
		if(j < 0) {
			j = s.indexOf(" ");
		}
		return s.substring(0, j);
	}
	return 0;
}

function browser_parse_version(name) {
	var v = browser_parse_version0(name + "/");
	if(!v) {
		v = browser_parse_version0(name + " ");
	}
	return v;
}

// Returns mozilla version for mozilla and compatible browsers.
function browser_mozilla_version() {
	var s = navigator.userAgent;

	// indexOf is buggy in Netscape 3
	if(s.lastIndexOf("Mozilla/3.") == 0) {
		return 3;
	} else if(s.lastIndexOf("Mozilla/") == 0) {
		return s.substring(8, s.indexOf("."));
	} else {
		return 0;
	}
}

// Returns browser version in Opera, 0 in other browsers.
function browser_Opera() {
	return browser_parse_version("Opera");
}

// Returns mozilla version in Netscape, 0 in other browsers.
function browser_NetscapeMozilla() {
	var brz = navigator.userAgent;
	var compat = brz.toLowerCase().lastIndexOf("compatible") >= 0;
	var opera = browser_Opera();
	if(brz.lastIndexOf("Mozilla/") == 0 && !compat && !opera) {
		return browser_mozilla_version();
	} else {
		return 0;
	}
}

// Returns browser version in MSIE, 0 in other browsers.
function browser_MSIE() {
	var msie = navigator.appName.lastIndexOf("Microsoft Internet Explorer") == 0;
	var opera = browser_Opera();
	if(msie && !opera) {
		return browser_parse_version("MSIE");
	}
	return 0;
}

function browser_Chrome(){
	return navigator.userAgent.indexOf("Chrome")>=0;
}

//Returns the OS version (9 or 10) if it is mac, 0 if isn't.
function macOsVer() {
	var v = navigator.appVersion;
	var vv = navigator.userAgent.toLowerCase();
	var mac = 0;
	if(v.indexOf("Mac") > 0) {
		mac = 9;
		if(vv.indexOf("os x") > 0) {
			mac = 10;
		}
	}
	return mac;
}

function isLeopardSafari() {
    var agent = navigator.userAgent;
    var isLeopard = agent.lastIndexOf("Intel Mac OS X 10_5") > 0 || agent.lastIndexOf("Intel Mac OS X 10.5") > 0 || agent.lastIndexOf("Intel Mac OS X 10_6") > 0 || agent.lastIndexOf("Intel Mac OS X 10.6") > 0;
    if(isLeopard) {
        return agent.lastIndexOf("Safari/") > 0;
    }
    return false;
}

function marvin_default_jvm()
{
    var osver = macOsVer();
    var mozver = browser_NetscapeMozilla();
    // Mac always use built-in Java
    // Netscape 4 prefers built-in Java
    if(osver >= 9 || mozver == 4) {
        return "builtin";
    } else {
        return "plugin";
    }
}

// Determines default GUI (Swing or AWT) from JVM and browser type.
function marvin_default_gui(jvm)
{
    var osver = macOsVer();
    var mozver = browser_NetscapeMozilla();
    // Only OS 9 and Netscape 4 uses AWT
    if(osver == 9 || mozver == 4) {
	return "awt"
    } else {
	return "swing";
    }
}

var mayscrDefined = false;

// "" string if no problem, error message if applet initalization has been failed
var java_error_msg = getJavaErrorMessage(checkJava());

function applet_begin(jvm, codebase, archive, code, width, height, name, mayscr)
{
	if(java_error_msg != "") {
            _appletstrbuf =  "<p><strong>"+java_error_msg+"</strong></p>";
            return _appletstrbuf;
        }
	var netscape = browser_NetscapeMozilla();
	var msie = browser_MSIE();
	var opera = browser_Opera();
	var chrome = browser_Chrome();
	applet_type = 0; // <applet>
	if(jvm == "plugin") {
		if((netscape || opera) && ! chrome) {
			applet_type = 1; // <embed> in Netscape, Opera and Mozilla
		} else if(msie) {
			applet_type = 2; // <object> in Microsoft
		}
	}
	var s;
	if(applet_type == 1) {
		s = '<embed TYPE="application/x-java-applet;version=1.6"\n';
		s += ' PLUGINSPAGE="https://java.sun.com/javase/downloads/index.jsp"\n';
	} else if(applet_type == 2) {
		s = '<object CLASSID="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93"\n'; // highest installed version of Java Plug-in.
		s += ' CODEBASE="https://java.sun.com/update/1.6.0/jinstall-6u25-windows-i586.cab#Version=1,6,0,0"\n';
	} else {
		s = '<applet';
	}
	if(mayscr) {
		if(applet_type == 0) { // <applet>
			s += ' MAYSCRIPT';
		} else if(applet_type == 1) { // <embed>
			s += ' MAYSCRIPT=true';
		}
                mayscrDefined = true;
	}
	s += ' WIDTH='+width+' HEIGHT='+height;
	if(name) {
		s += ' ID="'+name+'" NAME="'+name+'"'; // define name attribute to refer with document.MSketch, in other cases refer by id: document.getElementById("MSketch")
	}
	s += '\n';
	if(msketch_usedJars != "") { 	
		archive += "," + msketch_usedJars;
	}
	if(applet_type != 2) { // <applet> and <embed>
		s += ' CODEBASE="'+codebase+'" ARCHIVE="'+archive+'" CODE="'+code+'"';
	}
	if(applet_type != 1) { // <applet> and <object>
		s += '>\n';
        }
	if(applet_type == 2) { // <object>
		s += '<param NAME="codebase" VALUE="'+codebase+'">\n';
		s += '<param NAME="archive" VALUE="'+archive+'">\n';
		s += '<param NAME="code" VALUE="'+code+'">\n';
		s += '<param NAME="scriptable" VALUE="true">\n';
		if(mayscr) {
			s += '<param NAME="mayscript" VALUE="true">\n';
                        mayscrDefined = true;
		}
	}
	_appletstrbuf = s;
	return s;
}

var skinDefined = false;
var isSetLegacy = false;
function applet_param(name, value)
{
	if(java_error_msg != "") {
		return;
	}
	var s;
        if(name == "skin") {
            // do not overwrite skin later
            skinDefined = true;
        }
        if(name != "legacy_lifecycle") {
            value="@javascript-URI-encoded@"+encodeURIComponent(value);
        } else {
            isSetLegacy=true;
        }
	if(applet_type == 1) { // <embed>
		s = ' '+name+'="'+value+'"\n';
	} else { // <applet> and <object>
		s = '<param NAME="'+name+'" VALUE="'+value+'">\n';
	} 
	_appletstrbuf += s;
	return s;
}

function applet_end(type)
{
	if(java_error_msg != "") {
		s0 = _appletstrbuf;
		if (!humanreadable){
			s0 = s0.replace(/\n/g,"");
		}
		return s0;
	}
	var s;
	var msg = "<center><b>YOU CANNOT SEE A JAVA APPLET HERE</b></center>\n";
        var legacy = !isLeopardSafari();
        if(type == 'msketch') {
            legacy = msketch_legacy;
        } else if(type == 'mview') {
            legacy = mview_legacy;
        }

	if(applet_type == 1) { // <embed>
            if(!isSetLegacy && legacy) {
                s = ' legacy_lifecycle="true"\n';
            } else {
                s = '';                
            }
                s += ' java-vm-args="-Djnlp.packEnabled=true -Xmx512m"\n';
		s += ' codebase_lookup="false"\n';
		s += '>\n<noembed>\n';
		s += msg;
		s += '</noembed>\n';
	} else if(applet_type == 2) { // <object>
                if(!isSetLegacy && legacy) {
                    s = '<param name="legacy_lifecycle" value="true"/>\n';
                } else {
                    s = '';
                }
                s += '<param name="java-vm-args" value="-Djnlp.packEnabled=true -Xmx512m"/>\n';
		s += '<param name="codebase_lookup" value="false"/>\n';
		s += msg;
		s += '</object>\n';
	} else { // <applet>
                if(!isSetLegacy && legacy) {
                    s = '<param name="legacy_lifecycle" value="true"/>\n';
                } else {
                    s = '';
                }
                s += '<param name="java-vm-args" value="-Djnlp.packEnabled=true -Xmx512m"/>\n';
                if(mayscrDefined && !skinDefined && isLeopardSafari()) {
                    s += '<param name="skin" value="javax.swing.plaf.metal.MetalLookAndFeel"/>\n'+msg;
                } else {
		    s += msg;
                }        
		s += '<param name="codebase_lookup" value="false"/>\n';        
		s += '</applet>\n';
	}
	_appletstrbuf += s;
  	s = _appletstrbuf;
   	_appletstrbuf = "";
	if (!humanreadable){
		s = s.replace(/\n/g,"");
	}
	return s;
}


//
// "Public" functions
//


// Determine the JVM.
function marvin_get_jvm() {
	var jvm = marvin_jvm0;
	if(!jvm) {
		jvm = (marvin_jvm != "")? marvin_jvm : marvin_default_jvm();
	}
	jvm = jvm.toLowerCase();
	return jvm;
}

// Determine GUI type ("awt" or "swing").
function marvin_get_gui() {
	var gui = marvin_gui0;
	if(!gui) {
		gui = (marvin_gui != "")? marvin_gui : marvin_default_gui(marvin_get_jvm());
	}
	return gui;
}

// If msketch is able to generate image returns 1 else 0. It is depends on 
// the browser.
function msketch_detect() 
{
	var netscape = browser_NetscapeMozilla();
	var msie = browser_MSIE();
	if(msie > 0) {
	    marvin_jvm = "plugin";
	} else if(netscape > 0) {
	    if(netscape > 4) {
	        marvin_jvm = "builtin";
	    } else {
	    	alert("Image generation can be run only in SWING mode.\n"+
			"Your browser does not support SWING.");
		return 0;
	    }
	}
	return 1;
}

function msketch_begin(codebase, width, height){
	msketch_begin(codebase, width, height, isLeopardSafari());
}

function msketch_begin(codebase, width, height, oldbehaviour)
{
        if(oldbehaviour == undefined) {
            oldbehaviour = isLeopardSafari();
        }
	var archive, code;
	var jvm = marvin_get_jvm();
	var gui = marvin_get_gui();
	if (oldbehaviour){
		code = "chemaxon/marvin/applet/JMSketch";
	} else {
		code = "chemaxon/marvin/applet/JMSketchLaunch";
	}
	if(gui.toLowerCase() == "swing") {
		if (oldbehaviour){
			archive = "jmarvin.jar";
		} else {
			archive = "appletlaunch.jar"
		}
	} else {
		return;
//		archive = "marvin.jar";
//		code = "MSketch";
	}
	applet_begin(jvm, codebase, archive, code, width, height, msketch_name,
		     msketch_mayscript);
}


function msketch_param(name, value)
{
	return applet_param(name, value);
}

function msketch_end()
{
	s0 = msketch_end_to_string();
	document.write(s0);
}

function msketch_end_to_string() {
	s0 = applet_end("msketch");
	msketch_name = "";
	return s0;
}

function mview_begin(codebase, width, height){
	mview_begin(codebase, width, height, isLeopardSafari());
}

function mview_begin(codebase, width, height, oldbehaviour)
{
        if(oldbehaviour == undefined) {
            oldbehaviour = isLeopardSafari();
        }
	var archive, code;
	var jvm = marvin_get_jvm();
	var gui = marvin_get_gui();
	if(gui.toLowerCase() == "swing") {
		if (oldbehaviour){
	    	archive = "jmarvin.jar";
			code = "chemaxon/marvin/applet/JMView";
		} else {
			archive = "appletlaunch.jar";
			code = "chemaxon/marvin/applet/JMViewLaunch";
		}
	} else {
		return;
//		archive = "marvin.jar";
//		code = "MView";
	}
	applet_begin(jvm, codebase, archive, code, width, height, mview_name,
		     mview_mayscript);
}

function mview_param(name, value)
{
	return applet_param(name, value);
}

function mview_end_to_string()
{
	s0 = applet_end("mview");
	mview_name = "";
	return s0;
}

function mview_end() {
	s0 = mview_end_to_string();
	document.write(s0);
}

function links_set_search(s) {
	for(i = 0; i < document.links.length; ++i) {
		var p = document.links[i].pathname;
		if(p.lastIndexOf(".html") > 0 || p.lastIndexOf(".jsp") > 0) {
			var href = document.links[i].href;
			var k = href.indexOf('?');
			if(k > 0) {
				href = href.substring(0, k);
			}
			document.links[i].href = href + s;
		}
	}
}

function unix2local(s) {
	var strvalue = "" + s;
	var v = navigator.appVersion;
	if(v.indexOf("Win") > 0) {
		strvalue = strvalue.split("\r\n").join("\n"); // To avoid "\r\r\n"
		return strvalue.split("\n").join("\r\n");
	} else { // Unix
		return strvalue;
	}
}

function local2unix(s) {
	var strvalue = "" + s;
	var v = navigator.appVersion;
	if(v.indexOf("Win") > 0) {
		return strvalue.split("\r").join("");
	} else { // Unix
		return strvalue;
	}
}

