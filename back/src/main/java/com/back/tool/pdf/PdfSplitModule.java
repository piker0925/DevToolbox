package com.back.tool.pdf;

import com.back.tool.model.ToolInput;
import com.back.tool.model.ToolModule;
import com.back.tool.model.ToolParams;
import com.back.tool.model.ToolProcessingException;
import com.back.tool.model.ToolResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class PdfSplitModule implements ToolModule {

    /** 1부터 시작하는 양 끝 포함 페이지 범위 */
    record PageRange(int start, int end) {}

    @Override
    public String getId() { return "pdf-split"; }

    @Override
    public String getName() { return "PDF 분할"; }

    @Override
    public String getCategory() { return "pdf"; }

    @Override
    public boolean isHeavy() { return true; }

    @Override
    public ToolResult process(ToolInput input) {
        ToolParams params = ToolParams.of(input);
        String pageRangeSpec = params.getString("pageRange", "");
        String groupMode = params.getString("groupMode", "page");
        if (!groupMode.equals("page") && !groupMode.equals("range")) {
            throw new ToolProcessingException(
                    "분할 방식은 page(페이지별 1파일) 또는 range(범위별 1파일)여야 합니다. (입력값: " + groupMode + ")");
        }

        try {
            Path inputPdf = input.files().get(0);
            Path output = Files.createTempFile("pdfsplit-", ".zip");

            try (PDDocument doc = PDDocument.load(inputPdf.toFile())) {
                int totalPages = doc.getNumberOfPages();
                List<PageRange> ranges = pageRangeSpec.isBlank()
                        ? List.of(new PageRange(1, totalPages))
                        : parsePageRanges(pageRangeSpec, totalPages);

                try (OutputStream fos = Files.newOutputStream(output);
                     ZipOutputStream zip = new ZipOutputStream(fos)) {
                    if (groupMode.equals("page")) {
                        writePerPage(doc, ranges, zip);
                    } else {
                        writePerRange(doc, ranges, zip);
                    }
                }
            }
            return ToolResult.ofFile(output);
        } catch (IOException e) {
            throw new ToolProcessingException("PDF 분할 실패: " + e.getMessage(), e);
        }
    }

    /** 페이지별 1파일: page-004.pdf 처럼 원본 페이지 번호로 이름 지정 (중복 페이지는 1회만) */
    private void writePerPage(PDDocument doc, List<PageRange> ranges, ZipOutputStream zip) throws IOException {
        Set<Integer> pages = new LinkedHashSet<>();
        for (PageRange range : ranges) {
            for (int p = range.start(); p <= range.end(); p++) pages.add(p);
        }
        for (int p : pages) {
            writePages(doc, p, p, zip, String.format("page-%03d.pdf", p));
        }
    }

    /** 범위별 1파일: pages-001-003.pdf (단일 페이지 범위는 page-005.pdf) */
    private void writePerRange(PDDocument doc, List<PageRange> ranges, ZipOutputStream zip) throws IOException {
        Set<String> usedNames = new LinkedHashSet<>();
        for (PageRange range : ranges) {
            String name = range.start() == range.end()
                    ? String.format("page-%03d.pdf", range.start())
                    : String.format("pages-%03d-%03d.pdf", range.start(), range.end());
            if (!usedNames.add(name)) continue; // 동일 범위 중복 입력은 1회만
            writePages(doc, range.start(), range.end(), zip, name);
        }
    }

    private void writePages(PDDocument src, int start, int end, ZipOutputStream zip, String entryName)
            throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PDDocument out = new PDDocument()) {
            for (int p = start; p <= end; p++) {
                out.importPage(src.getPage(p - 1));
            }
            out.save(buffer);
        }
        zip.putNextEntry(new ZipEntry(entryName));
        zip.write(buffer.toByteArray());
        zip.closeEntry();
    }

    /**
     * '1-3,5,7-' 형식의 페이지 범위 문법을 파싱한다.
     * 열린 범위('7-')의 끝과 총 페이지를 초과하는 끝 페이지는 총 페이지로 조정한다.
     */
    static List<PageRange> parsePageRanges(String spec, int totalPages) {
        List<PageRange> ranges = new ArrayList<>();
        for (String token : spec.split(",", -1)) {
            String t = token.trim();
            if (t.isEmpty()) {
                throw new ToolProcessingException(
                        "페이지 범위에 빈 항목이 있습니다: '" + spec + "' (예: 1-3,5)");
            }
            int start;
            int end;
            int dash = t.indexOf('-');
            if (dash >= 0) {
                if (t.indexOf('-', dash + 1) >= 0) {
                    throw new ToolProcessingException(
                            "페이지 범위 형식이 잘못되었습니다: '" + t + "' (예: 1-3,5)");
                }
                start = parsePageNumber(t.substring(0, dash), t);
                String endPart = t.substring(dash + 1).trim();
                end = endPart.isEmpty() ? totalPages : parsePageNumber(endPart, t);
            } else {
                start = parsePageNumber(t, t);
                end = start;
            }
            if (start < 1) {
                throw new ToolProcessingException("페이지 번호는 1 이상이어야 합니다: '" + t + "'");
            }
            if (start > totalPages) {
                throw new ToolProcessingException(
                        "시작 페이지(" + start + ")가 문서의 총 페이지 수(" + totalPages + ")를 초과합니다.");
            }
            if (end < start) {
                throw new ToolProcessingException(
                        "시작 페이지가 끝 페이지보다 큽니다: '" + t + "'");
            }
            if (end > totalPages) {
                end = totalPages; // 초과분은 유효 범위로 조정
            }
            ranges.add(new PageRange(start, end));
        }
        return ranges;
    }

    private static int parsePageNumber(String value, String token) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ToolProcessingException(
                    "페이지 범위 형식이 잘못되었습니다: '" + token + "' (예: 1-3,5)");
        }
    }
}
