package com.github.aha.poc.itext;

import static com.itextpdf.kernel.pdf.EncryptionConstants.ALLOW_PRINTING;
import static com.itextpdf.kernel.pdf.EncryptionConstants.ENCRYPTION_AES_256;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

@DisplayName("iText security features")
public class EncryptionTests extends AbstractTest {

	public static final String RESULT = RESULT_PATH + "/encryption.pdf";

	@Test
	@DisplayName("add password and restrict (remove) copy permission")
	public void testEncryption() throws Exception {
		try (Document document = new Document(createDocument(RESULT))) {
			Paragraph paragraph = new Paragraph("secured");
			paragraph.setFixedPosition(20, 0, 100);
			document.add(paragraph);
		}
	}

	private PdfDocument createDocument(String targetFilename) throws IOException {
		PdfReader reader = new PdfReader("hello-source.pdf");
		WriterProperties wp = buildWriterProperties();
		wp.setStandardEncryption("aha".getBytes(), null, ALLOW_PRINTING /* | EncryptionConstants.ALLOW_COPY */, ENCRYPTION_AES_256);
		PdfWriter writer = new PdfWriter(RESULT, wp);
		return new PdfDocument(reader, writer);
	}

}