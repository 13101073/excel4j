package molaga.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.junit.Test;

/**
 * @since 2017-06-07
 * @version 1.0
 * @description 
 *     使用POI生成Excel
 * @usage 
 *     调用<b> {@link #make(List, String[], String)} </b>来导入数据
 *     需要输出时，请最后调用<b> {@link #flush(OutputStream)} </b>
 * */
public class PoiXixi {
	private static ThreadLocal<HSSFWorkbook> WB = new ThreadLocal<HSSFWorkbook>();

	/*JUnit test case*/
	@Test
	public void test() throws FileNotFoundException {
		//全默认参数示例
		make(null, null, null);
		//全参数示例
		make(
				Arrays.asList(new Employee[] { new Employee("liyujia"),new Employee("xixix") }), 
				new String[]{"列名1","列名2","列名3"}, 
				"工作簿名1"
			);
		//数据参数不为空示例
		make(
				Arrays.asList(new Employee[] { new Employee("liyujia2"),new Employee("xixix2") }), 
				null, 
				null
			);
		flush(new FileOutputStream("test2.xls"));
	}

	/**
	 * @param os
	 *            输出流，默认使用文件输出，支持输出到HttpResponse 
	 *            注意：本方法不会关闭流，所以根据情况输出流可能需要手动关闭，
	 * */
	public void flush(OutputStream os) {
		HSSFWorkbook wb = WB.get();// 获取workbook
		if (wb == null) {
			throw new IllegalStateException(
					"构建Excel失败：输出到流之前请先使用PoiXixi#make方法创建Excel！");
		}
		try {
			if (os == null)
				os = new FileOutputStream("temp.xls");
			wb.write(os);
			os.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				wb.close();
				WB.set(null); //clear.
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 生成excel
	 * @param data
	 *            数据，默认为空使用测试参数
	 *            （需要注意List中数据的class，有待完善）
	 * @param titles
	 *            数据的列标题，可以不填
	 * @param sName
	 *            工作簿名称，可以不填
	 * */
	public static void make(List<?> data, String[] titles, String sName) {
		try {

			int brs = 1; // body row start - 正文内容起始行号
			int cr = brs; // current row - 当前的行号

			HSSFWorkbook wb = WB.get();// 创建excel
			if (wb == null) {
				wb = new HSSFWorkbook();
				WB.set(wb);
			}
			if (sName == null || "".equals(sName)) {
				sName = Long.toString(System.currentTimeMillis());
			}
			HSSFSheet sheet = wb.createSheet(sName);// 创建一个工作薄
			setSheetColumn(sheet);// 设置工作薄列宽
			HSSFRow row = null; // 行
			HSSFCell c = null; // 单元格

			if (data == null)
				data = genTestData();
			if (titles == null) {
				titles = genDefaultTitles(data.get(0));
			}
			writeTitleContent(sheet, createTitleCellStyle(wb), titles);// 写入标题

			// 正文中数据内容的默认样式
			HSSFCellStyle bodyStyle = createBodyCellStyle(wb);
			HSSFCellStyle dateStyle = createDateBodyCellStyle(wb);
			for (int i = 0; i < data.size(); i++) {
				Object o = data.get(i);
				Field[] f = o.getClass().getDeclaredFields();

				// 第二行写开始写入正文内容
				row = sheet.createRow(cr);
				for (int j = 0; j < f.length; j++) {
					f[j].setAccessible(true);
					Object val = f[j].get(o);
					c = row.createCell(j);
					if(val == null) {
					    continue;
					}
					if (val instanceof Date) {
						c.setCellStyle(dateStyle);
						c.setCellValue((Date) val);
					} else {
						c.setCellStyle(bodyStyle);
						c.setCellValue(val.toString());
					}
				}
				cr++;// 当前行号递增1
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 默认标题头，使用数据类型的field名
	 * */
	protected static String[] genDefaultTitles(Object o) {
		Field[] f = o.getClass().getDeclaredFields();

		String[] t = new String[f.length];
		for (int i = 0; i < f.length; i++) {
			t[i] = f[i].getName();
		}
		return t;
	}

	/**
	 * 设置工作簿列宽
	 * */
	protected static void setSheetColumn(HSSFSheet sheet) {
		// TODO 可定制
		sheet.setDefaultColumnWidth(24);
	}

	/**
	 * 设置正文单元样式
	 */
	protected static HSSFCellStyle createBodyCellStyle(HSSFWorkbook wb) {
		HSSFCellStyle cStyle = wb.createCellStyle();
		HSSFFont font = wb.createFont();
		font.setFontHeightInPoints((short) 8);
		font.setFontName(HSSFFont.FONT_ARIAL);// 设置标题字体
		cStyle.setFont(font);
		cStyle = wb.createCellStyle();
		cStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 居中
		return cStyle;
	}

	/**
	 * 设置正文单元时间样式
	 * 
	 * @param workbook
	 * @return
	 */
	protected static HSSFCellStyle createDateBodyCellStyle(HSSFWorkbook wb) {
		HSSFCellStyle cStyle = wb.createCellStyle();
		HSSFFont font = wb.createFont();
		font.setFontHeightInPoints((short) 8);
		font.setFontName(HSSFFont.FONT_ARIAL);// 设置标题字体
		cStyle.setFont(font);
		cStyle = wb.createCellStyle();
		cStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 居中
		HSSFDataFormat format = wb.createDataFormat();
		cStyle.setDataFormat(format.getFormat("yyyy-mm-dd"));
		return cStyle;
	}

	/**
	 * 设置标题单元样式
	 * 
	 * @param wb
	 * @return
	 */
	protected static HSSFCellStyle createTitleCellStyle(HSSFWorkbook wb) {
		HSSFCellStyle cStyle = wb.createCellStyle();
		HSSFFont font = wb.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		font.setFontHeightInPoints((short) 8);
		font.setFontName(HSSFFont.FONT_ARIAL);// 设置标题字体
		cStyle.setFont(font);
		cStyle = wb.createCellStyle();
		cStyle.setFont(font);// 设置列标题样式
		cStyle.setFillForegroundColor(HSSFColor.SKY_BLUE.index);// 设置背景色
		cStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER); // 居中
		return cStyle;
	}

	/**
	 * 写入标题行
	 * 
	 * @param sheet
	 *            要写入的Sheet
	 * @param cStyle
	 *            默认使用<b> {@link #createTitleCellStyle} </b>样式
	 */
	protected static void writeTitleContent(HSSFSheet sheet,
			HSSFCellStyle cStyle, String[] titles) {
		if (sheet == null) {
			throw new IllegalArgumentException();
		}
		if (cStyle == null) {
			cStyle = createTitleCellStyle(sheet.getWorkbook());
		}
		// 标题行
		HSSFRow r = sheet.createRow(0);
		HSSFCell c;
		// 写入标题行
		if (titles == null) {
			throw new IllegalArgumentException(
					"构建Excel失败：标题参数{name: titles; type: String[]}不能为{null}");
		}
		for (int i = 0; i < titles.length; i++) {
			c = r.createCell(i);
			c.setCellStyle(cStyle);
			c.setCellValue(titles[i]);
		}

	}

	/**
	 * @deprecated 生产测试用数据
	 * */
	private static List<Employee> genTestData() {
		ArrayList<Employee> l = new ArrayList<Employee>();
		l.add(new Employee("张三"));
		l.add(new Employee("李四"));
		l.add(new Employee("就酱"));
		return l;
	}

	/**
	 * @deprecated 测试用数据类型
	 * */
	static class Employee {
		private String name;
		private Date birthDate = new Date();
		private long payment = 123L;

		Employee(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getBirthDate() {
			return birthDate;
		}

		public void setBirthDate(Date birthDate) {
			this.birthDate = birthDate;
		}

		public long getPayment() {
			return payment;
		}

		public void setPayment(long payment) {
			this.payment = payment;
		}
	}
}
