package dataProvider.internetProvider.handlers;

import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class which parses xml and retrieves a session key
 * @author mikalaj
 *
 */
public class ConnectHandler extends DefaultHandler{
	private Stack<String> nodes;
	private String sessid;
	public ConnectHandler(){
		nodes = new Stack<String>();
	}	
	@Override
	public void startElement(String uri, String localName, String qName,Attributes atts)
	throws SAXException {	
		nodes.push(localName);
	}
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String st = new String(ch, start, length);
		String lastNode = nodes.peek();		
		if(lastNode.equalsIgnoreCase("sessid")){
			sessid = st;
		}			
	}
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {		
			nodes.pop();
	}
	public String getSessid(){
		return sessid;
	}
}
