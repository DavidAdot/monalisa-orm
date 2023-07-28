public class CsvOptions {
	private String separator = ",";
	private Character quotechar = '"';
	private String commentChar;
	private int skipLeadingLines = 0;
	private boolean trimHeaders = true;
	private boolean ignoreUnparseableLines;
	private int skipLeadingDataLines;
	private boolean trimValues = true;
	private boolean suppressHeaders = false;
	private boolean headerFixedWidth = true;
	private String quoteStyle = "C";
	private boolean defectiveHeaders;
	private ArrayList<int[]> fixedWidthColumns = null;
	private String headerLine;
	private String missingValue;
	private String charset = "utf-8";

	private CsvOptions() {}

	public static CsvOptions createDefaultOptions() {
		return new CsvOptions();
	}

	// Setters for individual properties

	String getSeparator() {
		return separator;
	}

	Character getQuotechar() {
		return quotechar;
	}

	String getCommentChar() {
		return commentChar;
	}

	int getSkipLeadingLines() {
		return skipLeadingLines;
	}

	boolean isTrimHeaders() {
		return trimHeaders;
	}

	boolean isIgnoreUnparseableLines() {
		return ignoreUnparseableLines;
	}

	int getSkipLeadingDataLines() {
		return skipLeadingDataLines;
	}

	boolean isTrimValues() {
		return trimValues;
	}

	boolean isSuppressHeaders() {
		return suppressHeaders;
	}

	boolean isHeaderFixedWidth() {
		return headerFixedWidth;
	}

	String getQuoteStyle() {
		return quoteStyle;
	}

	boolean isDefectiveHeaders() {
		return defectiveHeaders;
	}

	ArrayList<int[]> getFixedWidthColumns() {
		return fixedWidthColumns;
	}

	String getHeaderLine() {
		return headerLine;
	}

	String getMissingValue() {
		return missingValue;
	}

	String getCharset() {
		return charset;
	}

	// Setters for individual properties (package-private)

	void setSeparator(String separator) {
		this.separator = separator;
	}

	void setQuotechar(Character quotechar) {
		this.quotechar = quotechar;
	}

	void setCommentChar(String commentChar) {
		this.commentChar = commentChar;
	}

	void setSkipLeadingLines(int skipLeadingLines) {
		this.skipLeadingLines = skipLeadingLines;
	}

	void setTrimHeaders(boolean trimHeaders) {
		this.trimHeaders = trimHeaders;
	}

	void setIgnoreUnparseableLines(boolean ignoreUnparseableLines) {
		this.ignoreUnparseableLines = ignoreUnparseableLines;
	}

	void setSkipLeadingDataLines(int skipLeadingDataLines) {
		this.skipLeadingDataLines = skipLeadingDataLines;
	}

	void setTrimValues(boolean trimValues) {
		this.trimValues = trimValues;
	}

	void setSuppressHeaders(boolean suppressHeaders) {
		this.suppressHeaders = suppressHeaders;
	}

	void setHeaderFixedWidth(boolean headerFixedWidth) {
		this.headerFixedWidth = headerFixedWidth;
	}

	void setQuoteStyle(String quoteStyle) {
		this.quoteStyle = quoteStyle;
	}

	void setDefectiveHeaders(boolean defectiveHeaders) {
		this.defectiveHeaders = defectiveHeaders;
	}

	void setFixedWidthColumns(ArrayList<int[]> fixedWidthColumns) {
		this.fixedWidthColumns = fixedWidthColumns;
	}

	void setHeaderLine(String headerLine) {
		this.headerLine = headerLine;
	}

	void setMissingValue(String missingValue) {
		this.missingValue = missingValue;
	}

	void setCharset(String charset) {
		this.charset = charset;
	}
}
