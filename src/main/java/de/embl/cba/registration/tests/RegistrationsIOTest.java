package de.embl.cba.registration.tests;

import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RegistrationsIOTest {


    public static void simpleXmlTest() throws IOException
    {
        Element root = new Element("r");
        Document doc = new Document(root);
        Element inner = new Element("inner");
        root.setAttribute("attribute", "value");
        root.addContent(inner);
        inner.addContent("inner content");
        root.addContent(new Comment("comment text"));
        root.addContent("some inline text");
        root.addContent(new Element("inner2"));

        new XMLOutputter( Format.getPrettyFormat()).output(doc, System.out);

        String outputFilePath = "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/text.xml";
        File outputFile = new File(outputFilePath);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        new XMLOutputter( Format.getPrettyFormat()).output(doc, fileOutputStream);

    }


    public static void main(String[] args) throws IOException
    {

        simpleXmlTest();

    }

}
