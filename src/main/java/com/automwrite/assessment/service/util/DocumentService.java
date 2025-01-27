package com.automwrite.assessment.service.util;

//import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.util.Units;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

//import org.apache.poi.xwpf.usermodel.*;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;


@Slf4j
@Service
public class DocumentService {

    /**
     * Saves the provided XWPFDocument to a specified file name.
     *
     * @param document The XWPFDocument to be saved
     * @throws IOException If an error occurs during the file writing process
     */
    public void saveDocument(XWPFDocument document) throws IOException {
        try (FileOutputStream out = new FileOutputStream("recommendation.docx")) {
            document.write(out);
        }
    }

    /**
     * Loads a predefined template from the resources folder.
     *
     * @return The loaded XWPFDocument template
     * @throws IOException If the template file is not found or cannot be loaded
     */
    public XWPFDocument loadTemplate() throws IOException {
        try (InputStream templateStream = getClass().getResourceAsStream("/templates/recommendation-template_Jay.docx")) {
            if (templateStream == null) {
                throw new IOException("Template file not found");
            }
            return new XWPFDocument(templateStream);
        }
    }



	public void insertContentIntoTemplate(XWPFDocument templateDocument, Map<String, String>[] processedContent) {

		Map<String, String> currentPlan = processedContent[0];
		Map<String, String> advisorDetails = processedContent[1];
		Map<String, String> targetProvider = processedContent[2];
		Map<String, String> clientDetails = processedContent[3];

		String currProvider = currentPlan.get ("provider");
		String planId = currentPlan.get ("planId");
		String planValue = currentPlan.get ("planValue");
		String targetPlan = targetProvider.get ("provider");
		String targetOrg = targetProvider.get ("organizationName");
	
		String advisor1 = advisorDetails.get ("name") + ",";
		String advisor2 = advisorDetails.get ("id") + ",";
		String advisor3 = advisorDetails.get ("email") + ",";
	
		String sub = "RE: Plan transfer from " + currProvider + " to " + targetPlan;
		String recommendation1 = "To help you transfer from " + currProvider + " to " + "the desired " +
		targetPlan + "'s platform,  we recommend you the following: "; 
	
		String recommendation2 = "1. I will contact " + targetPlan + " platform provider organization, " + 
		targetOrg + ", to confirm eligibility and specific requirements.";

		String recommendation3 = "2. I will Request transfer documents, and complete the necessary paperwork from both financial institutions";
	
		String recommendation4 = "3. I want to Ensure you understand potential fees of the transfer";
	
		String recommendation5 = "4. " + targetProvider.get ("organizationName") + "\'s " +
									targetProvider.get ("portfolio") + " carries an annual maintenance fee of " + targetProvider.get ("annualFee") + ", with a minimumm investment of £" + targetProvider.get("minInvestment");
	

    	String fee_str = targetProvider.get ("fees");
		int ind1 = fee_str.indexOf("250000.01");
		int ind2 = fee_str.indexOf("500000.00");
		int ind3 = fee_str.indexOf("0.20%");
		int ind4 = fee_str.indexOf("500000.01");
		int ind5 = fee_str.indexOf("0.15%");
	
		String recommendation6 = "5. " + targetProvider.get ("provider") + " platform\'s fees are as follows:";
		String recommendation7 = "	1. " + fee_str.substring (0,1) + " to " + " £" + fee_str.substring (6, 15) +
									" is " + fee_str.substring (17, 22);
	
		String recommendation8 = "	2. " + "£" + fee_str.substring (ind1,ind1+"250000.01".length()) + " to " + " £" + fee_str.substring (ind2, ind2+"500000.00".length()) +
									" is " + fee_str.substring (ind3,ind3+"0.20%".length());
		
		String recommendation9 = "	3. " + "from £" + fee_str.substring (ind4,ind4+"500001.00".length()) + " up is " + fee_str.substring (ind5, ind5+"0.15%".length());

    	LocalDate currentDate = LocalDate.now();
    	// Define the desired format
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    	// Convert LocalDate to String
    	String date = currentDate.format(formatter);

    	String outputFilePath = "output.docx";  // Path for the filled document


		for (XWPFParagraph paragraph : templateDocument.getParagraphs()) {
			StringBuilder fullText = new StringBuilder();
			for (XWPFRun run : paragraph.getRuns()) {
				String runText = run.getText(0);
				if (runText != null) {
					fullText.append(runText);
				}
			}

			String paragraphText = fullText.toString();
			if (paragraphText.contains("{Client Address}")) {
				String addr1 = clientDetails.get("address_street") + ","; 
				String addr2 = clientDetails.get("address_city") + " " + 
								clientDetails.get("address_state") + " " +
								clientDetails.get("address_zipCode") + ",";
				String addr3 = clientDetails.get("address_country");
	
				String updatedText = paragraphText.replace("{Client Address}", addr1);
				log.debug("Found and replaced placeholder with: {}", addr1);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
	
    			// Add the second address line in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(addr2);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
	
    			// Add the third address line in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(addr3);
	
	
			}
			if (paragraphText.contains("RE: Your recommendation letter")) {
				String updatedText = paragraphText.replace("RE: Your recommendation letter", sub);
				log.debug("Found and replaced placeholder with: {}", sub);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
				newRun.setBold(true);  
			}
			if (paragraphText.contains("{Client Name}")) {
				String name = "Mr. " + clientDetails.get("firstName") + " " + clientDetails.get("lastName") + ",";
				String updatedText = paragraphText.replace("{Client Name}", name);
				log.debug("Found and replaced placeholder with: {}", name);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
                newRun.setBold(true);
			}
			if (paragraphText.contains("{Date}")) {
	
				String updatedText = paragraphText.replace("{Date}", date);
				log.debug("Found and replaced placeholder with: {}", date);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
                newRun.setBold(true);
			}
			if (paragraphText.contains("// TODO INSERT ADVICE HERE")) {
				String updatedText = paragraphText.replace("// TODO INSERT ADVICE HERE", recommendation1);
				log.debug("Found and replaced placeholder with: {}", recommendation1);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation2);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation3);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation4);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation5);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation6);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation7);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation8);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(recommendation9);
	
	
	
	
	
			}
			if (paragraphText.contains("{Current Provider}")) {
				String updatedText = paragraphText.replace("{current Provider}", currProvider);
				log.debug("Found and replaced placeholder with: {}", currProvider);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
			}
			if (paragraphText.contains("{Advisor Phone}")) {
				String phone = advisorDetails.get ("phone");
				String updatedText = paragraphText.replace("{Advisor Phone}", phone);
				log.debug("Found and replaced placeholder with: {}", phone);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
			}
			if (paragraphText.contains("{Advisor Details}")) {
				String name = advisorDetails.get ("name");
				String updatedText = paragraphText.replace("{Advisor Details}", advisor1);
				log.debug("Found and replaced placeholder with: {}", advisor1);
	
				// Clear existing runs and set updated text
				for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
					paragraph.removeRun(i);
				}
	
				XWPFRun newRun = paragraph.createRun();
				newRun.setText(updatedText);
                newRun.setBold(true);	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(advisor2);
	
				// Add a line break within the same paragraph
    			newRun.addBreak();  // This will insert a line break
    	
    			// Add the second recommendation in a new run
    			newRun = paragraph.createRun();
    			newRun.setText(advisor3);
	
			}
		}


		// Iterate through all tables in the document
        for (XWPFTable table : templateDocument.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String cellText = cell.getText();

                    // Replace placeholders if they exist
                    if (cellText.contains("{Current Provider}")) {
                        cellText = cellText.replace("{Current Provider}", currProvider);
                        updateCellText(cell, cellText);
                    }
                    if (cellText.contains("{Policy Id}")) {
                        cellText = cellText.replace("{Policy Id}", planId);
                        updateCellText(cell, cellText);
                    }
                    if (cellText.contains("{value}")) {
                        cellText = cellText.replace("{value}", planValue);
                        updateCellText(cell, cellText);
                    }
                    if (cellText.contains("{Target provider}")) {
                        cellText = cellText.replace("{Target provider}", targetPlan);
                        updateCellText(cell, cellText);
                    }
                    if (cellText.contains("{Recommendation}")) {
                        cellText = cellText.replace("{Recommendation}", "Please see below");
                        updateCellText(cell, cellText);
                    }
                }
            }
        }


        // Write the updated document to a new file
        log.debug ("Writing to file {}", "output.docx"); 
        try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
            templateDocument.write(fos);
        } catch (IOException e) { 
            e.printStackTrace();
        }
	}



	private void updateCellText(XWPFTableCell cell, String updatedText) {

		cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);

    	// Remove all paragraphs in the cell
    	while (cell.getParagraphs().size() > 0) {
        	cell.removeParagraph(0);
    	}
    
    	// Add a new paragraph with the updated text
    	XWPFParagraph paragraph = cell.addParagraph();
    	XWPFRun run = paragraph.createRun();
    	run.setText(updatedText);
		run.setBold(true);
	}

}
