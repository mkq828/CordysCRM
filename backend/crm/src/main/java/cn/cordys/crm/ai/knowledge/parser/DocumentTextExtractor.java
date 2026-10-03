package cn.cordys.crm.ai.knowledge.parser;

import cn.cordys.common.exception.GenericException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文档文本抽取：按文件类型分发到 PDFBox（PDF）、POI（Word .docx）或纯文本直读（md/txt）。
 * 只支持文本型文档；扫描件/图片型 PDF 或加密文档解析不出文本时抛出业务异常。
 */
@Component
@Slf4j
public class DocumentTextExtractor {

    private static final String PARSE_FAIL_MESSAGE = "解析失败，暂不支持扫描件/图片型 PDF 或加密文档，请上传文本型文档";

    public String extract(MultipartFile file, String fileType) {
        try {
            String text = switch (fileType) {
                case "pdf" -> extractPdf(file.getInputStream());
                case "docx" -> extractDocx(file.getInputStream());
                case "md", "txt" -> new String(file.getBytes(), StandardCharsets.UTF_8);
                default -> throw new GenericException("不支持的文件类型");
            };
            if (text == null || text.isBlank()) {
                throw new GenericException(PARSE_FAIL_MESSAGE);
            }
            return text;
        } catch (GenericException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败，fileType={}", fileType, e);
            throw new GenericException(PARSE_FAIL_MESSAGE);
        }
    }

    private String extractPdf(InputStream in) throws Exception {
        try (var doc = Loader.loadPDF(in.readAllBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
