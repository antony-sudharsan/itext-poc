package com.github.aha.poc.itext;

import static com.itextpdf.io.font.constants.StandardFonts.HELVETICA;
import static com.itextpdf.kernel.pdf.EncryptionConstants.ALLOW_PRINTING;
import static com.itextpdf.kernel.pdf.EncryptionConstants.ENCRYPTION_AES_256;
import static com.itextpdf.kernel.pdf.PdfVersion.PDF_2_0;
import static com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination.createFit;
import static java.lang.Math.PI;
import static java.util.Objects.nonNull;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.barcodes.Barcode39;
import com.itextpdf.barcodes.BarcodeEAN;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DocumentBuilder {

	@NonNull
	String targetFile;

	@Setter
	PdfVersion pdfVersion = PDF_2_0;

	Document document;

	public void init() {
		document = new Document(createDocument(targetFile, buildWriterProperties(pdfVersion)));
	}

	public void initWithPassword(byte[] userPassword, byte[] ownerPassword) {
		WriterProperties writerProperties = buildWriterProperties(pdfVersion);
		writerProperties.setStandardEncryption(userPassword, ownerPassword, ALLOW_PRINTING /* | EncryptionConstants.ALLOW_COPY */,
				ENCRYPTION_AES_256);
		document = new Document(createDocument(targetFile, writerProperties));
	}

	public void generateDocument() {
		document.close();
	}

	PdfDocument createDocument(String targetFilename, WriterProperties writerProperties) {
		try {
			PdfWriter writer = createPdfWriter(targetFilename, writerProperties);
			return new PdfDocument(writer);
		} catch (FileNotFoundException e) {
			log.error("Creating PDF failed", e);
			throw new ITextException(e.getMessage());
		}
	}

	PdfWriter createPdfWriter(String targetFilename, WriterProperties writerProperties) throws FileNotFoundException {
		return new PdfWriter(targetFilename, writerProperties); // NOSONAR
	}

	WriterProperties buildWriterProperties(PdfVersion version) {
		WriterProperties wp = new WriterProperties();
		wp.addXmpMetadata();
		wp.setPdfVersion(version);
		return wp;
	}

	public void addTitle(String text) {
		Paragraph titleElement = createParagraph(text);
		titleElement.setBold();
		document.add(titleElement);
		addMetadata(text, null, null, null);
	}

	public void addParagraph(String text) {
		document.add(createParagraph(text));
	}

	public void addParagraph(Paragraph paragraph) {
		document.add(paragraph);
	}

	public void addBarcode39(String code) {
		document.add(createBarcode39Image(code));
	}

	public void addBarcode128(String code) {
		document.add(createBarcode128Image(code));
	}

	public void addBarcodeEAN(String code) {
		document.add(createBarcodeEANImage(code));
	}

	public void addQrCode(String code) {
		document.add(createQrCodeImage(code));
		document.add(createParagraph(code));
	}

	public void addMetadata(String title, String subject, String author, String creator) {
		PdfDocumentInfo documentInfo = document.getPdfDocument().getDocumentInfo();
		if (nonNull(title)) {
			documentInfo.setTitle(title);
		}
		if (nonNull(subject)) {
			documentInfo.setSubject(subject);
		}
		if (nonNull(author)) {
			documentInfo.setAuthor(author);
		}
		if (nonNull(creator)) {
			documentInfo.setCreator(creator);
		}
	}

	public void addCustomMetadadata(@NonNull String key, @NonNull String value) {
		PdfDocumentInfo documentInfo = document.getPdfDocument().getDocumentInfo();
		documentInfo.setMoreInfo(key, value);
	}

	public void addWatermark(String watermark) {
		float fontSize = 100;
		Paragraph paragraph = createStyledParagraph(watermark, ParagraphStyle.builder().fontName(HELVETICA).fontSize(fontSize).build());

		PdfExtGState transparentGraphicState = new PdfExtGState().setFillOpacity(0.5f);

		for (int i = 1; i <= document.getPdfDocument().getNumberOfPages(); i++) {
			addWatermarkToPage(i, paragraph, transparentGraphicState, fontSize);
		}
	}

	public void addBookmark(String title) {
		PdfOutline outlines = document.getPdfDocument().getOutlines(false);
		PdfOutline newOutline = outlines.addOutline(title);
		newOutline.addDestination(createFit(document.getPdfDocument().getLastPage()));
	}

	Paragraph createStyledParagraph(String content, ParagraphStyle paragraphStyle) {
		Paragraph paragraph = new Paragraph(content);
		if (nonNull(paragraphStyle.getFontName())) {
			paragraph.setFont(createFont(paragraphStyle.getFontName()));
		}
		if (nonNull(paragraphStyle.getFontSize())) {
			paragraph.setFontSize(paragraphStyle.getFontSize());
		}
		if (nonNull(paragraphStyle.getRotation())) {
			paragraph.setRotationAngle(calculateRadiusFromDegree(paragraphStyle.getRotation()));
		}
		if (nonNull(paragraphStyle.getBorder())) {
			paragraph.setBorder(paragraphStyle.getBorder());
		}
		return paragraph;
	}

	private double calculateRadiusFromDegree(Float rotation) {
		// half rotation in Radians is Pi (3.14) -> full rotation is 2 Pi
		return PI / 180 * rotation;
	}

	public Text createStyledText(String label, TextStyle textStyle) {
		Text text = new Text(label);
		if (nonNull(textStyle.getColor())) {
			text.setFontColor(textStyle.getColor());
		}
		if (nonNull(textStyle.getBackgroundColor())) {
			text.setBackgroundColor(textStyle.getBackgroundColor());
		}
		if (nonNull(textStyle.getFontFamily())) {
				text.setFont(createFont(textStyle.getFontFamily()));
		}
		if (nonNull(textStyle.getFontSize())) {
			text.setFontSize(textStyle.getFontSize());
		}
		if (textStyle.isBold()) {
			text.setBold();
		}
		if (textStyle.isItalic()) {
			text.setItalic();
		}
		if (textStyle.isUnderline()) {
			text.setUnderline();
		}
		if (textStyle.isLineThrough()) {
			text.setLineThrough();
		}
		return text;
	}

	PdfFont createFont(String fontType) {
		try {
			return PdfFontFactory.createFont(fontType);
		} catch (IOException e) {
			throw new ITextException("Font creation failed", e);
		}
	}

	private void addWatermarkToPage(int pageIndex, Paragraph paragraph, PdfExtGState graphicState, float fontSize) {
		PdfDocument pdfDoc = document.getPdfDocument();
		PdfPage pdfPage = pdfDoc.getPage(pageIndex);
		Rectangle pageSize = pdfPage.getPageSizeWithRotation();

		float x = (pageSize.getLeft() + pageSize.getRight()) / 2;
		float y = (pageSize.getTop() + pageSize.getBottom()) / 2;
		PdfCanvas over = new PdfCanvas(pdfDoc.getPage(pageIndex));
		over.saveState();
		over.setExtGState(graphicState);
		float xOffset = fontSize / 2;
		document.showTextAligned(paragraph, x - xOffset, y, pageIndex, TextAlignment.CENTER, VerticalAlignment.TOP, 45);
		over.restoreState();
	}

	private float getPageWidth() {
		return document.getPdfDocument().getFirstPage().getPageSizeWithRotation().getWidth();
	}

	public Paragraph createParagraph(String text) {
		return new Paragraph(text);
	}

	Image createBarcode39Image(String code) {
		Barcode39 codeObject = new Barcode39(document.getPdfDocument());
		codeObject.setCode(code);
		PdfFormXObject codeImage = codeObject.createFormXObject(document.getPdfDocument());
		return createCodeImage(codeImage, false);
	}

	Image createBarcode128Image(String code) {
		Barcode128 codeObject = new Barcode128(document.getPdfDocument());
		codeObject.setCode(code);
		PdfFormXObject codeImage = codeObject.createFormXObject(document.getPdfDocument());
		return createCodeImage(codeImage, false);
	}

	Image createBarcodeEANImage(String code) {
		BarcodeEAN codeObject = new BarcodeEAN(document.getPdfDocument());
		codeObject.setCode(code);
		PdfFormXObject codeImage = codeObject.createFormXObject(document.getPdfDocument());
		return createCodeImage(codeImage, false);
	}

	Image createQrCodeImage(String code) {
		BarcodeQRCode codeObject = new BarcodeQRCode(code);
		PdfFormXObject codeImage = codeObject.createFormXObject(document.getPdfDocument());
		return createCodeImage(codeImage, true);
	}

	private Image createCodeImage(PdfFormXObject codeImage, boolean setWidth) {
		Image codeQrImage = new Image(codeImage);
		if (setWidth) {
			codeQrImage.setWidth(getPageWidth() / 4);
		}
		return codeQrImage;
	}

}
