package com.sakura.formly.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public final class FormDefinitionPasteParseUtil {

    private static final int EDITOR_DEFAULT_COLUMN_WIDTH = 120;
    private static final int EDITOR_DEFAULT_ROW_HEIGHT = 25;

    private FormDefinitionPasteParseUtil() {
    }

    /**
     * 统一收口粘贴解析入口：优先 HTML 表格，失败后回退到纯文本表格。
     */
    public static Object parseSchema(String documentHtml, String plainText) {
        // 1. 先按首版策略提取一张可用表格。
        ParsedTable parsedTable = parsePastedTable(documentHtml, plainText);

        // 2. 没识别到表格时直接返回 null，交给上层决定提示文案。
        if (ObjectUtil.isNull(parsedTable)) {
            return null;
        }

        // 3. 将表格结果转换成编辑器 schema。
        return buildPastedSchema(parsedTable);
    }

    /**
     * 保持首版策略简单明确，只取第一张有效表格。
     */
    private static ParsedTable parsePastedTable(String documentHtml, String plainText) {
        // 1. 优先解析浏览器剪贴板里的 HTML 表格。
        ParsedTable htmlTable = parseHtmlTable(documentHtml);

        if (isValidParsedTable(htmlTable)) {
            return htmlTable;
        }

        // 2. HTML 没有可用表格时，再回退到纯文本的换行/Tab 表格。
        ParsedTable plainTextTable = parsePlainTextTable(plainText);

        if (isValidParsedTable(plainTextTable)) {
            return plainTextTable;
        }

        // 3. 两种来源都没有识别出表格时返回 null。
        return null;
    }

    /**
     * 解析剪贴板里的 HTML，只提取第一张有效表格。
     */
    private static ParsedTable parseHtmlTable(String documentHtml) {
        // 1. 空 HTML 直接跳过，避免进入解析器。
        if (StrUtil.isBlank(documentHtml)) {
            return null;
        }

        // 2. 通过 Jsoup 提取第一张 table 元素。
        Element table = Jsoup.parse(documentHtml).selectFirst("table");
        if (ObjectUtil.isNull(table)) {
            return null;
        }

        // 3. 遍历行列，提取单元格文本与合并信息。
        List<ParsedRow> rows = new ArrayList<>();

        for (Element tr : table.select("tr")) {
            List<ParsedCell> cells = tr.select("> td, > th").stream()
                    .map(td -> new ParsedCell(
                            cleanCellText(td.wholeText()),
                            parseIntAttr(td, "rowspan"),
                            parseIntAttr(td, "colspan")
                    ))
                    .toList();

            if (CollUtil.isNotEmpty(cells)) {
                rows.add(new ParsedRow(cells));
            }
        }

        // 4. 没有有效行时返回 null。
        if (CollUtil.isEmpty(rows)) {
            return null;
        }

        return new ParsedTable(rows);
    }

    /**
     * 解析 HTML 元素上的整型属性，异常或缺失时退回 1。
     */
    private static int parseIntAttr(Element element, String attrName) {
        String value = element.attr(attrName);
        if (StrUtil.isBlank(value)) {
            return 1;
        }

        try {
            return Math.max(Integer.parseInt(value), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * 解析纯文本表格，按“换行分行、Tab 分列”的首版规则处理。
     */
    private static ParsedTable parsePlainTextTable(String plainText) {
        // 1. 首版纯文本必须至少包含 Tab，否则不认为是表格。
        if (StrUtil.isBlank(plainText) || !StrUtil.contains(plainText, '\t')) {
            return null;
        }

        // 2. 先统一换行符，避免 Windows/Office 粘贴内容影响分行。
        String normalizedPlainText = StrUtil.replace(StrUtil.replace(plainText, "\r\n", "\n"), "\r", "\n");

        // 3. 逐行拆分，再按 Tab 拆列，生成最基础的单元格结构。
        List<ParsedRow> rows = Arrays.stream(normalizedPlainText.split("\n", -1))
                .filter(StrUtil::isNotBlank)
                .map(line -> {
                    String[] columns = line.split("\t", -1);

                    if (columns.length == 0) {
                        return null;
                    }

                    List<ParsedCell> cells = Arrays.stream(columns)
                            .map(column -> new ParsedCell(cleanCellText(column), 1, 1))
                            .toList();
                    return new ParsedRow(cells);
                })
                .filter(ObjectUtil::isNotNull)
                .toList();

        // 4. 拆分后没有任何有效行，就视为解析失败。
        if (CollUtil.isEmpty(rows)) {
            return null;
        }

        // 5. 返回纯文本解析结果。
        return new ParsedTable(rows);
    }

    /**
     * 判断解析结果是否真的是一张可用表格。
     */
    private static boolean isValidParsedTable(ParsedTable parsedTable) {
        // 1. 先判断表格对象和行集合是否存在。
        if (ObjectUtil.isNull(parsedTable) || CollUtil.isEmpty(parsedTable.rows())) {
            return false;
        }

        // 2. 至少要有一行带单元格，才算有效结果。
        return parsedTable.rows().stream().anyMatch(row -> CollUtil.isNotEmpty(row.cells()));
    }

    /**
     * 将解析结果转成编辑器 schema，并按当前协议补齐空白格与合并格从属单元格。
     */
    private static Object buildPastedSchema(ParsedTable parsedTable) {
        List<SchemaCell> schemaCells = new ArrayList<>();
        List<List<Boolean>> occupiedGrid = new ArrayList<>();
        int maxColumnCount = 0;

        // 1. 逐行逐格写入主单元格，并同步记录被合并区域占用的位置。
        for (int rowIndex = 0; rowIndex < parsedTable.rows().size(); rowIndex += 1) {
            ParsedRow parsedRow = parsedTable.rows().get(rowIndex);
            int columnIndex = 0;

            for (ParsedCell parsedCell : parsedRow.cells()) {
                while (isGridOccupied(occupiedGrid, rowIndex, columnIndex)) {
                    columnIndex += 1;
                }

                String parentCellId = createCellId();
                int rowSpan = Math.max(parsedCell.rowSpan(), 1);
                int colSpan = Math.max(parsedCell.colSpan(), 1);

                schemaCells.add(new SchemaCell(
                        parentCellId,
                        rowIndex + 1,
                        columnIndex + 1,
                        rowSpan,
                        colSpan,
                        false,
                        "",
                        buildTextFields(cleanCellText(parsedCell.text()))
                ));

                for (int rowOffset = 0; rowOffset < rowSpan; rowOffset += 1) {
                    for (int colOffset = 0; colOffset < colSpan; colOffset += 1) {
                        markGridOccupied(occupiedGrid, rowIndex + rowOffset, columnIndex + colOffset);

                        if (rowOffset == 0 && colOffset == 0) {
                            continue;
                        }

                        schemaCells.add(new SchemaCell(
                                createCellId(),
                                rowIndex + rowOffset + 1,
                                columnIndex + colOffset + 1,
                                1,
                                1,
                                true,
                                parentCellId,
                                List.of()
                        ));
                    }
                }

                maxColumnCount = Math.max(maxColumnCount, columnIndex + colSpan);
                columnIndex += colSpan;
            }
        }

        int rowCount = occupiedGrid.size();

        // 2. 补齐解析结果里未显式出现的空白普通单元格，保证 schema 是完整矩阵。
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex += 1) {
            for (int columnIndex = 0; columnIndex < maxColumnCount; columnIndex += 1) {
                // 已被占用的位置（主单元格或合并区域）直接跳过。
                if (isGridOccupied(occupiedGrid, rowIndex, columnIndex)) {
                    continue;
                }

                schemaCells.add(new SchemaCell(
                        createCellId(),
                        rowIndex + 1,
                        columnIndex + 1,
                        1,
                        1,
                        false,
                        "",
                        List.of()
                ));
            }
        }

        // 3. 按编辑器约定的行列顺序排序，避免后续渲染和比较出现抖动。
        schemaCells.sort(Comparator.comparingInt(SchemaCell::row).thenComparingInt(SchemaCell::col));

        // 4. 组装最终 schema，行高列宽沿用当前编辑器默认值。
        LinkedHashMap<String, Object> schema = new LinkedHashMap<>();
        schema.put("rows", rowCount);
        schema.put("columns", maxColumnCount);
        schema.put("cells", schemaCells.stream().map(FormDefinitionPasteParseUtil::toSchemaCell).toList());
        schema.put("columnWidths", buildFixedSizes(maxColumnCount, EDITOR_DEFAULT_COLUMN_WIDTH));
        schema.put("rowHeights", buildFixedSizes(rowCount, EDITOR_DEFAULT_ROW_HEIGHT));
        return schema;
    }

    /**
     * 判断某个网格位置是否已经被前面的单元格或合并区域占用。
     */
    private static boolean isGridOccupied(List<List<Boolean>> occupiedGrid, int rowIndex, int columnIndex) {
        if (rowIndex >= occupiedGrid.size()) {
            return false;
        }

        List<Boolean> row = occupiedGrid.get(rowIndex);
        if (columnIndex >= row.size()) {
            return false;
        }

        return Boolean.TRUE.equals(row.get(columnIndex));
    }

    /**
     * 将某个位置标记为已占用，供后续跳过合并格覆盖区域。
     */
    private static void markGridOccupied(List<List<Boolean>> occupiedGrid, int rowIndex, int columnIndex) {
        // 1. 先补齐目标行。
        while (occupiedGrid.size() <= rowIndex) {
            occupiedGrid.add(new ArrayList<>());
        }

        List<Boolean> row = occupiedGrid.get(rowIndex);

        // 2. 再补齐目标列。
        while (row.size() <= columnIndex) {
            row.add(false);
        }

        // 3. 标记当前位置已被占用。
        row.set(columnIndex, true);
    }

    /**
     * 生成固定长度的默认行高/列宽数组。
     */
    private static List<Integer> buildFixedSizes(int size, int value) {
        return IntStream.range(0, size)
                .mapToObj(index -> value)
                .toList();
    }

    /**
     * 将非空文本转换成编辑器里的固定文字组件。
     */
    private static List<Object> buildTextFields(String text) {
        if (StrUtil.isBlank(text)) {
            return List.of();
        }

        // 1. 按当前编辑器最小协议补齐固定文字组件字段。
        LinkedHashMap<String, Object> field = new LinkedHashMap<>();
        field.put("uuid", IdUtil.fastSimpleUUID().substring(0, 8));
        field.put("type", "text");
        field.put("placeholder", "");
        field.put("required", false);
        field.put("helpText", "");
        field.put("horizontalAlign", "left");
        field.put("textContent", text);
        field.put("imageUrl", "");
        field.put("options", List.of());
        field.put("switchActiveText", "");
        field.put("switchInactiveText", "");
        return List.of(field);
    }

    /**
     * 将内部单元格结构转换成前端编辑器需要的 cell JSON。
     */
    private static Object toSchemaCell(SchemaCell schemaCell) {
        LinkedHashMap<String, Object> cell = new LinkedHashMap<>();
        cell.put("id", schemaCell.id());
        cell.put("row", schemaCell.row());
        cell.put("col", schemaCell.col());
        cell.put("fields", schemaCell.fields());
        cell.put("rowSpan", schemaCell.rowSpan());
        cell.put("colSpan", schemaCell.colSpan());
        cell.put("merged", schemaCell.merged());
        cell.put("mergeParentId", schemaCell.mergeParentId());
        return cell;
    }

    /**
     * 生成与前端当前协议一致的 cell id。
     */
    private static String createCellId() {
        return "cell_" + IdUtil.fastSimpleUUID().substring(0, 8);
    }

    /**
     * 清洗单元格文本：处理 nbsp、压缩行内空白，并保留换行。
     */
    private static String cleanCellText(String rawText) {
        // 1. 空内容直接返回，避免产生无意义组件。
        if (StrUtil.isBlank(rawText)) {
            return "";
        }

        // 2. 先按换行拆开，每一行单独做空白清洗。
        String[] lines = StrUtil.replace(rawText, "\r", "").split("\n", -1);

        // 3. 保留换行结构，只压缩行内多余空白。
        return Arrays.stream(lines)
                .map(line -> StrUtil.replace(line, "\u00A0", " "))
                .map(line -> line.replaceAll("[\\t\\x0B\\f ]+", " ").trim())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .strip();
    }

    private record ParsedCell(String text, int rowSpan, int colSpan) {
    }

    private record ParsedRow(List<ParsedCell> cells) {
    }

    private record ParsedTable(List<ParsedRow> rows) {
    }

    private record SchemaCell(
            String id,
            int row,
            int col,
            int rowSpan,
            int colSpan,
            boolean merged,
            String mergeParentId,
            List<Object> fields
    ) {
    }


}
