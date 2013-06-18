package hudson.plugins.android_emulator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ParseXml {
	public void modifyFile(File xmlFile, String node, String attributeName, String attributeValue) {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document doc = null;
		try {
			docBuilder = dbFactory.newDocumentBuilder();
			doc = docBuilder.parse(xmlFile);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		doc.getDocumentElement().normalize();
		Element rootNode = doc.getDocumentElement();
		Element element = null;		
		NodeList nodeList = doc.getElementsByTagName(node);  
		
			List<String> nodeValue = new ArrayList<String>();
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node n = nodeList.item(i);  				
				if(n.getNodeType()==Node.ELEMENT_NODE){
					element = (Element) n;
					nodeValue.add(element.getAttribute(attributeName));					  				  					  
				}			  			 
			}
			boolean present = nodeExists(nodeValue,attributeValue);
			if(!present){
				if(node.equals("uses-permission")) {
					Element ele = doc.createElement(node);
					ele.setAttribute(attributeName, attributeValue);
					rootNode.appendChild(ele);
					writeToXml(doc,xmlFile);
				} else if(node.equals("instrumentation")) {
					element.setAttribute(attributeName,attributeValue);
					writeToXml(doc,xmlFile);
				}
			}			
		}
	}
	
	private boolean nodeExists(List<String> list, String attributeValue) {
		for(String str : list){
			if(str.contains(attributeValue))
				return true;
		}
		return false;		
	}
	
	private void writeToXml(Document document, File xmlFile) {
		DOMSource source = new DOMSource(document);
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
			StreamResult result = new StreamResult(xmlFile);
			transformer.transform(source, result);
			} catch (TransformerConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}