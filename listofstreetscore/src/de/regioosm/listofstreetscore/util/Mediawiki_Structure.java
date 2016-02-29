package de.regioosm.listofstreetscore.util;

/**
 * A single input source for an blabla on LogMessage
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 */

/*

	V1.1, 19.02.2013, Dietmar Seifert
		*	class was not in production.
		*	now the message will be stored in files in filesystem

	V1.0, 18.12.2012, Dietmar Seifert
		* store Messages about Informations, Warnings and Errors into database

*/


import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class Mediawiki_Structure {
	public HashMap<String,String> wddx_map = new HashMap<String,String>();

	public Object analyse_sections(Node act_node) {
		Object out_object = null;
		System.out.println(" aktueller Node ...");
		System.out.println("  .Name ===" + act_node.getNodeName() + "===");
		System.out.println("   .getTextContent() ===" + act_node.getTextContent() + "====");
		System.out.println("      .getNodeType() ===" + act_node.getNodeType() + "===");
		if(act_node.getNodeName().equals("struct")) {
			System.out.println(" Typ struct aktiv");
			NodeList sections = act_node.getChildNodes();
		    int numSections = sections.getLength();
		    for (int i = 0; i < numSections; i++) {
		    	Node act_subchild = (Node) sections.item(i);
				NamedNodeMap actsubchild_attributes = act_subchild.getAttributes();
				System.out.println(" node  Attributes (Number: "+actsubchild_attributes.getLength()+") are:");
				for(int attri = 0 ; attri<actsubchild_attributes.getLength() ; attri++) {
					Attr act_attribute = (Attr) actsubchild_attributes.item(attri);
					System.out.println("* [" + act_attribute.getName()+"] ==="+act_attribute.getValue()+"===");
				}
				analyse_sections(act_subchild);
		    }
		} else if(act_node.getNodeName().equals("var")) {
			System.out.println(" Typ var aktiv");
			NamedNodeMap actnode_attributes = act_node.getAttributes();
			Attr actnode_attribute_name = (Attr) actnode_attributes.getNamedItem("name");
			System.out.println("Attribut von act_node attribut name [" + actnode_attribute_name.getName()+"]==="+actnode_attribute_name.getValue()+"===");
			Node subchild0 = act_node.getChildNodes().item(0);
System.out.println("subchild0  .Name ===" + subchild0.getNodeName() + "===");
System.out.println("           .getTextContent() ===" + subchild0.getTextContent() + "====");
			if(subchild0.getNodeName().equals("string") || subchild0.getNodeName().equals("number")) {
				if( wddx_map.get(actnode_attribute_name.getValue()) == null) {
					wddx_map.put(actnode_attribute_name.getValue(), act_node.getTextContent());
					System.out.println("MAP entry set [" + actnode_attribute_name.getValue() + "] ===" + act_node.getTextContent() + "===");
				} else {
					System.out.println("Warning: MAP entry already set [" + actnode_attribute_name.getValue() + "]  with already text ==="+wddx_map.get(actnode_attribute_name.getValue())+"===   now ignored new value ===" + act_node.getTextContent() + "===");
				}
			} else {
				if( (! subchild0.getNodeName().equals("struct")) && 
					(! subchild0.getNodeName().equals("array"))) {
					System.out.println("ERROR ERROR: unknown sub-Elementtype, please check ==="+subchild0.getNodeName()+"===");
				}
				analyse_sections(subchild0);
			}
			//wddx_object[subchild0_attributes.getNamedItem("name")] = 
		} else if(	act_node.getNodeName().equals("wddxPacket") || 
					act_node.getNodeName().equals("header") || 
					act_node.getNodeName().equals("array") || 
					act_node.getNodeName().equals("data")) {
			NodeList sections = act_node.getChildNodes();
		    int numSections = sections.getLength();
		    for (int i = 0; i < numSections; i++) {
		    	Node subact_node = (Node) sections.item(i);
		    	analyse_sections(subact_node);
		    }
		} else {
			System.out.println(" Typ sonstiger, noch icht programmiert ==="+act_node.getNodeName()+"===");
		}
		return out_object; 
	}
	

	public void rekursiv_durchlaufen____NICHT_PRODUKTIV( String article_xml_content) {

		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document xml_document = null;

		System.out.println("Info: got this xml-content ===" + article_xml_content + "===");

		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			// parse xml-document.
			xml_document = builder.parse(new InputSource(new StringReader(article_xml_content)));
		} 
		catch (org.xml.sax.SAXException saxerror) {
			System.out.println("ERROR: SAX-Exception during parsing of xml-content ==="+article_xml_content+"===");
			saxerror.printStackTrace();
			return;
		} 
		catch (IOException ioerror) {
			System.out.println("ERROR: IO-Exception during parsing of xml-content ==="+article_xml_content+"===");
			ioerror.printStackTrace();
			return;
		}
		catch (ParserConfigurationException parseerror) {
			System.out.println("ERROR: fail to get new Instance from DocumentBuilderFactory");
			parseerror.printStackTrace();
		}

		NodeList sections = xml_document.getChildNodes();
	    int numSections = sections.getLength();
	    for (int i = 0; i < numSections; i++) {
	    	Node act_node = (Node) sections.item(i);
	    	analyse_sections(act_node);
	    }
		
	}		

	public HashMap<String, String> attributes( String article_xml_content, String xml_elementpath) {

		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document xml_document = null;

		HashMap<String, String> out_keyvalues = new HashMap<String, String>();

		System.out.println("Info: got this xml-content ===" + article_xml_content + "===");

		try {
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			// parse xml-document.
			xml_document = builder.parse(new InputSource(new StringReader(article_xml_content)));

			NodeList xmlelements = xml_document.getElementsByTagName(xml_elementpath);
		    int numSections = xmlelements.getLength();
		    for (int i = 0; i < numSections; i++) {
		    	Node act_node = (Node) xmlelements.item(i);
		    	NamedNodeMap attributes = act_node.getAttributes();
				System.out.println(" node  Attributes (Number: "+attributes.getLength()+") are:");
				for(int attri = 0 ; attri<attributes.getLength() ; attri++) {
					Attr act_attribute = (Attr) attributes.item(attri);
					System.out.println("* [" + act_attribute.getName()+"] ==="+act_attribute.getValue()+"===");
					if( out_keyvalues.get(act_attribute.getName()) == null)
						out_keyvalues.put(act_attribute.getName(), act_attribute.getValue());
				}
		    }
		
		} 
		catch (org.xml.sax.SAXException saxerror) {
			System.out.println("ERROR: SAX-Exception during parsing of xml-content ==="+article_xml_content+"===");
			saxerror.printStackTrace();
			return null;
		} 
		catch (IOException ioerror) {
			System.out.println("ERROR: IO-Exception during parsing of xml-content ==="+article_xml_content+"===");
			ioerror.printStackTrace();
			return null;
		}
		catch (ParserConfigurationException parseerror) {
			System.out.println("ERROR: fail to get new Instance from DocumentBuilderFactory");
			parseerror.printStackTrace();
		}

	    return out_keyvalues;
	}		

}
