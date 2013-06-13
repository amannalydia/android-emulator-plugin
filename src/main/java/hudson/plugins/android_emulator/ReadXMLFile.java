package hudson.plugins.android_emulator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;


public class ReadXMLFile {
	public String targetName;
	public String reportDir;
	public void ReadXML(String XmlFile){

		try{
		File fXmlFile = new File(XmlFile);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		
		doc.getDocumentElement().normalize();
		NodeList nList = doc.getElementsByTagName("target");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				targetName = eElement.getAttribute("name");
			}
		}
		NodeList rList = doc.getElementsByTagName("monkeytalk:run");
		for (int temp = 0; temp < rList.getLength(); temp++) {
			Node nNode = rList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				reportDir = eElement.getAttribute("reportdir");
				}
		}	
		}catch(Exception e) {
			e.printStackTrace();
	    }
	}
	public String getTargetName(){
		return targetName;
	}
	public String getReportDir(){
		return reportDir;
	}
}
