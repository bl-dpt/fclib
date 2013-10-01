/*
 * Copyright 2013 The SCAPE Project Consortium
 * Author: William Palmer (William.Palmer@bl.uk)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package uk.bl.dpt.fclib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to generate FOXML/METS for ingesting files in to Fedora Commons
 * @author wpalmer
 *
 */
public class FedoraIngestXMLGenerator {

	private int gFoxcount = 1;
	private String gCollection;// = "jisc1";
	private String gDatastreamID;// = "TIFF";
	private String gMimetype;// = "image/tiff";
	private String gChecksumType;// = "MD5";
	
	/**
	 * Initialise the class
	 * @param pCollection collection to use (e.g. "jisc1")
	 * @param pDatastreamID datastream identified for the files (e.g. TIFF)
	 * @param pMimetype mimetype for the files
	 * @param pChecksumType checksum type to use (use MD5)
	 */
	public FedoraIngestXMLGenerator(String pCollection, String pDatastreamID, String pMimetype, String pChecksumType) {
		gCollection = pCollection;
		gDatastreamID = pDatastreamID;
		gMimetype = pMimetype;
		gChecksumType = pChecksumType;
	}
	
	private void outputExiftool(PrintWriter pOut, String pOffset, String pFile) {
		List<String> commandLine = new ArrayList<String>();
		commandLine.add("/usr/bin/exiftool");
		commandLine.add("-X");//XML output
		commandLine.add(pFile);

		ProcessBuilder pb = new ProcessBuilder(commandLine);
		//don't redirect stderr to stdout as our output XML is in stdout
		pb.redirectErrorStream(false);

		//start the executable
		try {
			Process proc = pb.start();
			BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			proc.waitFor();
			//output xml to file
			while(stdout.ready()) {
				String line = stdout.readLine();
				if(line.contains("<?xml version='1.0'")) continue;//we don't want this 
				pOut.println(pOffset+line);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
			
	}
	
	private void outputDublinCore(PrintWriter pOut, String pOffset) {
		pOut.println(pOffset+"<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc\">"); 
		pOut.println(pOffset+"     <oai_dc:title>An image of a newspaper page</oai_dc:title>");			
		pOut.println(pOffset+"     <oai_dc:creator>British Library</oai_dc:creator>");			
		pOut.println(pOffset+"     <oai_dc:subject>Newspapers</oai_dc:subject>");			
		pOut.println(pOffset+"     <oai_dc:description>Large paper with text</oai_dc:description>");			
		pOut.println(pOffset+"     <oai_dc:publisher>British Library</oai_dc:publisher>");			
		//out.println(offset+"     <oai_dc:identifier>"+pid+"</oai_dc:identifier>");			
		pOut.println(pOffset+"</oai_dc:dc>");
	}
	
	private void outputSCAPEMETS(PrintWriter pOut, String pOffset, String pPid, String pFile, String pLabel) {
		pOut.println(pOffset+"<mets:mets OBJID=\""+pPid+"\"");//scape: optional
		pOut.println(pOffset+"           LABEL=\""+pLabel+"\"");//official desc of the object in fedora
		pOut.println(pOffset+"           PROFILE=\"SCAPE\"");
		pOut.println(pOffset+"           xmlns:mets=\"http://www.loc.gov/METS/\"");
		pOut.println(pOffset+"           xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
		pOut.println(pOffset+"           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		pOut.println(pOffset+"           xmlns:premis=\"info:lc/xmlns/premis-v2\"");
		pOut.println(pOffset+"           xsi:schemaLocation=\"http://www.loc.gov/METS_Profile/ http://www.loc.gov/standards/mets/profile_docs/mets.profile.v1-2.xsd");
		pOut.println(pOffset+"                                http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd");
	    pOut.println(pOffset+"                                http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-0.xsd\">");

		pOut.println(pOffset+"     <mets:metsHdr RECORDSTATUS=\"A\">");//createdate set by fedora on ingest
		pOut.println(pOffset+"          <mets:agent ROLE=\"IPOWNER\" TYPE=\"ORGANIZATION\">");
		pOut.println(pOffset+"               <mets:name>The British Library</mets:name>");
		pOut.println(pOffset+"          </mets:agent>");
		pOut.println(pOffset+"     </mets:metsHdr>");
		
		//scape: must use mdwrap or mdref; containing rights&provenance:PREMIS; source&descriptive:DC; technical:MIX for still images
		pOut.println(pOffset+"     <mets:amdSec ID=\"AS01\">");
		pOut.println(pOffset+"          <mets:techMD ID=\"object1\">");
		pOut.println(pOffset+"               <mets:mdWrap MIMETYPE=\"text/xml\" MDTYPE=\"PREMIS:OBJECT\" LABEL=\"Premis Object\">");
		pOut.println(pOffset+"                    <mets:xmlData>");
		pOut.println(pOffset+"                         <premis:object xsi:type=\"premis:file\"");
		pOut.println(pOffset+"                                        xsi:schemaLocation=\"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis-v2-0.xsd\">");
		pOut.println(pOffset+"                              <premis:originalName>"+pFile+"</premis:originalName>");			
		pOut.println(pOffset+"                         </premis:object>");			
		pOut.println(pOffset+"                    </mets:xmlData>");
		pOut.println(pOffset+"               </mets:mdWrap>");			
		pOut.println(pOffset+"          </mets:techMD>");			
		pOut.println(pOffset+"     </mets:amdSec>");			
		
		//scape: must use mdwrap or mdref; containing rights&provenance:PREMIS; source&descriptive:DC; technical:MIX for still images
		pOut.println(pOffset+"     <mets:dmdSec ID=\"DC\">");//scape: one and only one (note fedora uses dmdSecFedora, not dmdSec
		pOut.println(pOffset+"          <mets:descMD ID=\"DC1.0\">");			
		pOut.println(pOffset+"               <mets:mdWrap MIMETYPE=\"text/xml\" MDTYPE=\"DC\" LABEL=\"Dublin Core\">");
		pOut.println(pOffset+"                    <mets:xmlData>");
		outputDublinCore(pOut, pOffset+"                    "+"     ");
		pOut.println(pOffset+"                    </mets:xmlData>");
		pOut.println(pOffset+"               </mets:mdWrap>");
		pOut.println(pOffset+"          </mets:descMD>");			
		pOut.println(pOffset+"     </mets:dmdSec>");

//		out.println(offset+"     <mets:fileSec>");//must be names DATASTREAMS for fedora; scape: must have one per representation
//		out.println(offset+"          <mets:fileGrp ID=\"DATASTREAMS\">");//fedora doesn't like USE=\"master image\" 
//		out.println(offset+"               <mets:fileGrp ID=\"DS0\" STATUS=\"A\">");//fedora doesn't like USE=\"master image\"
//		out.println(offset+"                    <mets:file ID=\"DS0.0\" MIMETYPE=\""+mimetype+"\" SEQ=\"1\" OWNERID=\"M\"");//fedora needs ownerid
//	//	out.println(offset+"                               CHECKSUMTYPE=\""+checksumtype+"\" CHECKSUM=\""+checksum+"\">");
//	//	out.println(offset+"                         <mets:FLocat xlink:href=\""+url+"\" LOCTYPE=\"URL\"");//scape: must
//	//	out.println(offset+"                                      xlink:title=\""+label+"\"/>");
//		out.println(offset+"                    </mets:file>");
//		out.println(offset+"               </mets:fileGrp>");
//		out.println(offset+"          </mets:fileGrp>");
//		out.println(offset+"     </mets:fileSec>");

		pOut.println(pOffset+"     <mets:structMap ID=\"SM01\">");//scape: must have
		pOut.println(pOffset+"          <mets:div TYPE=\"book\" LABEL=\""+pLabel+"\">");
		pOut.println(pOffset+"               <mets:div TYPE=\"page\" LABEL=\"Page 1\">");
		pOut.println(pOffset+"                    <mets:fptr FILEID=\"TIFF\"/>");			
		pOut.println(pOffset+"               </mets:div>");			
		pOut.println(pOffset+"          </mets:div>");			
		pOut.println(pOffset+"     </mets:structMap>");
		
		pOut.println(pOffset+"</mets:mets>");
	}
	
	/**
	 * Create a FOXML record for Fedora ingest 
	 * @param pFile input tiff file
	 * @param pManaged whether Fedora should ingest the data (true) or reference the data (false)
	 * @param pOutputfile output file for generated FOXML
	 * @param pNewspaperMetadata whether to add newspaper metadata or not
	 */
	public void createFOXMLFromTIFF(String pFile, boolean pManaged, String pOutputfile, boolean pNewspaperMetadata) {

		String pid = gCollection+":"+gFoxcount;
		gFoxcount++;
		String imagefile = pFile;
		String url = "file://"+new File(imagefile).getAbsolutePath();
		String label = new File(imagefile).getName();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");//"2013-02-18T11:00:00.000Z";
		String date = df.format(new File(imagefile).lastModified());//utc iso8601

//		String checksum = null;
//		try {
//			checksum = Tools.generateChecksum(checksumtype, file);
//		} catch(IOException e) {
//			e.printStackTrace();
//			return;
//		}

		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pOutputfile)));//file+".foxml.xml")));

			out.println("<?xml version='1.0' encoding='ascii'?>");
			out.println("<foxml:digitalObject PID=\""+pid+"\"");
			out.println("                     VERSION=\"1.1\"");
			out.println("                     xmlns:foxml=\"info:fedora/fedora-system:def/foxml#\"");
			out.println("                     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
			out.println("                     xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd\">");

			out.println("     <foxml:objectProperties>");
			out.println("          <foxml:property NAME=\"info:fedora/fedora-system:def/model#state\" VALUE=\"A\"/>");
			out.println("          <foxml:property NAME=\"info:fedora/fedora-system:def/model#label\" VALUE=\""+label+"\"/>");
			out.println("          <foxml:property NAME=\"info:fedora/fedora-system:def/model#createdDate\" VALUE=\""+date+"\"/>");
			out.println("          <foxml:property NAME=\"info:fedora/fedora-system:def/view#lastModifiedDate\" VALUE=\""+date+"\"/>");
			out.println("     </foxml:objectProperties>");

			//dublin core
			out.println("     <foxml:datastream ID=\"DC\" CONTROL_GROUP=\"X\" STATE=\"A\">");
			out.println("          <foxml:datastreamVersion ID=\"DC.0\"");
			out.println("                                   CREATED=\""+date+"\"");
			out.println("                                   MIMETYPE=\"text/xml\"");
			out.println("                                   LABEL=\"Dublin Core\">");
			out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");
			out.println("               <foxml:xmlContent>");
			outputDublinCore(out, "                    ");
			out.println("               </foxml:xmlContent>");
			out.println("          </foxml:datastreamVersion>");
			out.println("     </foxml:datastream>");

			//fedora collection information
			out.println("     <foxml:datastream ID=\"RELS-EXT\" CONTROL_GROUP=\"X\">");
			out.println("          <foxml:datastreamVersion ID=\"RELS-EXT.0\"");
			out.println("                                   CREATED=\""+date+"\"");
			out.println("                                   MIMETYPE=\"text/xml\"");
			out.println("                                   LABEL=\"Fedora Collection ID\">");
			out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");
			out.println("               <foxml:xmlContent>");
			out.println("                    <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rel=\"info:fedora/fedora-system:def/relations-external#\">");
			out.println("                         <rdf:Description rdf:about=\"info:fedora/"+pid+"\">");
			out.println("                              <rel:isMemberOfCollection rdf:resource=\"info:fedora/"+gCollection+":"+gCollection+"\"/>");
			out.println("                         </rdf:Description>");
			out.println("                    </rdf:RDF>");
			out.println("               </foxml:xmlContent>");
			out.println("          </foxml:datastreamVersion>");
			out.println("     </foxml:datastream>");

			//image file
			out.println("     <foxml:datastream ID=\""+gDatastreamID+"\" CONTROL_GROUP=\""+(pManaged?"M":"E")+"\" STATE=\"A\">");
			out.println("          <foxml:datastreamVersion ID=\""+gDatastreamID+".0\"");
			out.println("                                   CREATED=\""+date+"\"");
			out.println("                                   MIMETYPE=\""+gMimetype+"\"");
			out.println("                                   LABEL=\""+label+"\">");
			//https://jira.duraspace.org/browse/FCREPO-787
			//we can't specify a checksum on ingest due to a bug in Fedora.  Just specifying a type works though.
			out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");// DIGEST=\""+checksum.toUpperCase()+"\"/>");
			out.println("               <foxml:contentLocation REF=\""+url+"\" TYPE=\"URL\"/>");
			out.println("          </foxml:datastreamVersion>");
			out.println("     </foxml:datastream>");

			//SCAPE METS record
			String metsID = "METS-SCAPE";
			String metsIDLabel = "SCAPE METS Record";
			out.println("     <foxml:datastream ID=\""+metsID+"\" CONTROL_GROUP=\"M\" STATE=\"A\">");
			out.println("          <foxml:datastreamVersion ID=\""+metsID+".0\"");
			out.println("                                   CREATED=\""+date+"\"");
			out.println("                                   MIMETYPE=\"text/xml\"");
			out.println("                                   LABEL=\""+metsIDLabel+"\">");
			out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");
			out.println("               <foxml:xmlContent>");
			outputSCAPEMETS(out, "                    ", pid, pFile, label);
			out.println("               </foxml:xmlContent>");
			out.println("          </foxml:datastreamVersion>");
			out.println("     </foxml:datastream>");

			//Exiftool output for the tiff
			out.println("     <foxml:datastream ID=\"EXIFTOOL\" CONTROL_GROUP=\"M\" STATE=\"A\">");
			out.println("          <foxml:datastreamVersion ID=\"EXIFTOOL.0\"");
			out.println("                                   CREATED=\""+date+"\"");
			out.println("                                   MIMETYPE=\"text/xml\"");
			out.println("                                   LABEL=\"Exiftool output for TIFF datastream\">");
			out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");
			out.println("               <foxml:xmlContent>");
			outputExiftool(out, "                    ", pFile);
			out.println("               </foxml:xmlContent>");
			out.println("          </foxml:datastreamVersion>");
			out.println("     </foxml:datastream>");
			
			if(pNewspaperMetadata) {
				String nmd = url.replace(".tif", ".xml");
				if(new File(nmd.substring("file://".length())).exists()) {
					out.println("     <foxml:datastream ID=\"BLNEWSPAPERMETADATA\" CONTROL_GROUP=\"M\" STATE=\"A\">");
					out.println("          <foxml:datastreamVersion ID=\"BLNEWSPAPERMETADATA.0\"");
					out.println("                                   CREATED=\""+date+"\"");
					out.println("                                   MIMETYPE=\"text/xml\"");
					out.println("                                   LABEL=\"Metadata and OCR for the image\">");
					out.println("               <foxml:contentDigest TYPE=\""+gChecksumType+"\"/>");
					out.println("               <foxml:contentLocation REF=\""+nmd+"\" TYPE=\"URL\"/>");
					out.println("          </foxml:datastreamVersion>");
					out.println("     </foxml:datastream>");
				} else {
					System.out.println("Error: newspaper metadata does not exist: "+nmd.substring("file://".length()));
				}
			}			

			out.println("</foxml:digitalObject>");

			out.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Test main method to generate FOXML for all parameters
	 * @param args arguments
	 */
	public static void main(String[] args) {
		System.out.println("Commands to ingest these files:");
		System.out.println("export FEDORA_HOME=/usr/local/fedora/");
		FedoraIngestXMLGenerator foxmlgen = new FedoraIngestXMLGenerator("jisc1", "TIFF", "image/tiff", "MD5");
		for(String file:args) {
			String foxml = new File(file).getName()+".foxml.xml";
			foxmlgen.createFOXMLFromTIFF(file, true, foxml, false);
			System.out.println("/usr/local/fedora/client/bin/fedora_ingest.sh f "+foxml+" info:fedora/fedora-system:FOXML-1.1 host:port user pass http");
		}

	}

}
