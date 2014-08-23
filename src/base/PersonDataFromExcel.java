package base;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PersonDataFromExcel {
	private HSSFWorkbook xwb = null;
	private TextFilter textFilter = null;
	//
	//private OPCPackage pkg = null;
	private XSSFWorkbook xwb2 = null;
	
	public PersonDataFromExcel() {
		textFilter = new TextFilter("E:/Code/java/CalcRegularityData/config");
	}	
	
	public PersonDataFromExcel(String path, String type) {
		if (type.equals("xls")) {
			try {
				xwb = new HSSFWorkbook(new FileInputStream(path));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 
		else if (type.equals("xlsx")) {
			try {
				//pkg  = OPCPackage.open(path);
				xwb2 = new XSSFWorkbook(new FileInputStream(path));
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
		textFilter = new TextFilter("E:/Code/java/CalcRegularityData/config");		
	}
	
	//获取excel工作簿中的表格数
	public int getExcelBookSheetNum() {
		if (xwb == null) {
			return 0;
		}		
		int n = xwb.getNumberOfSheets();
		return n;
	}
	public int getExcelBookSheetNum2() {
		if (xwb2 == null) {
			return 0;
		}		
		int n = xwb2.getNumberOfSheets();
		return n;
	}
	//读取某个sheet中的流水内容
	public HashMap<Long, ArrayList<FlowData>> readFlowDataFromSheet(int sheetNum) {
		HSSFSheet sheet = xwb.getSheetAt(sheetNum); //sheet
		HSSFRow row;
		HSSFCell cell;
		String str;
		String fullString;
		double val = 0.0;
		long uId = 0l;
		int cnt = 0;
		int beg = sheet.getFirstRowNum();
		int end = sheet.getLastRowNum();
		HashMap<Long, ArrayList<FlowData>> dataMap = new HashMap<Long, ArrayList<FlowData>>();
		for (int i = beg; i <= end; i++) {
			row  = sheet.getRow(i);							
			//==============================
			long timeStamp = 0l;
			long id = 0l;
			//full comment
			cell = row.getCell(14); 
			if (null == cell) {
				continue;
			}
			fullString = cell.toString();			
//			if (!textFilter.isValidDescriptionString(fullString)) {
//				continue;
//			}
//			if (fullString.equals("工资")) {
//				System.out.println(fullString);
//			}
			str = textFilter.getFilteredString(fullString);
			if (str.length() == 0) {
				continue;
			}
			FlowData flowData = new FlowData();			
			flowData.setCommentStr(str);
			flowData.setCommentFullStr(fullString);
			//id
			cell = row.getCell(0);
			val = cell.getNumericCellValue();
			id = new Double(val).longValue();
			flowData.setId(id);			
			//uid			 
			cell = row.getCell(1); 
			val = cell.getNumericCellValue();
			uId = new Double(val).longValue();
			flowData.setUid(uId);			
			//stamp
			cell = row.getCell(7); 
			val = cell.getNumericCellValue();
			timeStamp = new Double(val).longValue();
			flowData.setUnixStamp(timeStamp);
			//payType
			cell = row.getCell(9); 
			str = cell.toString();
			flowData.setPayType(str);
			//price
			cell = row.getCell(12); 
			val = cell.getNumericCellValue();
			flowData.setPrice(new Double(val).longValue());			
			//bankName
			cell = row.getCell(29);
			if (cell == null) {
				str = "xxxxxxx";
			} else {
				str = cell.toString();
			}			
			flowData.setBankNameStr(str);
			//Type
			cell = row.getCell(30); 
			str = cell.toString();
			flowData.setConsumeTypeStr(str);
			//cardid
			cell = row.getCell(3); 
			val = cell.getNumericCellValue();
			flowData.setCardid(new Double(val).intValue());		
			//=====
			String date = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timeStamp*1000));
			flowData.setDateStr(date);
			date = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(timeStamp*1000));
			System.out.println(date);
			flowData.setWeekString(date);
			cnt++;
			//add to map
			if (dataMap.containsKey(uId)) {
				ArrayList<FlowData> value = dataMap.get(uId);
				value.add(flowData);
			} else {
				ArrayList<FlowData> value = new ArrayList<FlowData>();
				value.add(flowData);
				dataMap.put(uId, value);
			}		
		}		
		System.out.println("总的纪录条数: " + end);
		System.out.println("筛选后的纪录条数: " + cnt);
		System.out.println("统计人数: " + dataMap.size());
		System.out.println("------------------------------");
		return dataMap;
	}
	
	//300万流水数据格式
	public HashMap<Long, ArrayList<FlowData>> readFlowDataFromSheet2(int sheetNum) {
		XSSFSheet sheet = xwb2.getSheetAt(sheetNum); //sheet
		XSSFRow row;
		XSSFCell cell;
		String str;
		String fullString;
		double val = 0.0;
		long uId = 0l;
		int cnt = 0;
		int beg = sheet.getFirstRowNum() + 1;
		int end = sheet.getLastRowNum();
		HashMap<Long, ArrayList<FlowData>> dataMap = new HashMap<Long, ArrayList<FlowData>>();
		for (int i = beg; i <= end; i++) {
			row  = sheet.getRow(i);							
			//==============================
			//filter
			//=========
			long timeStamp = 0l;
			long id = 0l;
			double privce = 0.0;
			//price
			cell = row.getCell(8);			
			privce = cell.getNumericCellValue();
			if (privce == 0) {
				continue;
			}
			//full comment
			cell = row.getCell(11); 
			if (null == cell) {
				continue;
			}
			fullString = cell.toString();						
			str = textFilter.getFilteredString(fullString);
			if (!textFilter.isContainKeyword(str)) {
				continue;
			}
//			if (str.length() == 0) {
//				str = "xxxxxx";
//				fullString = "xxxxxx";
//			}
			FlowData flowData = new FlowData();			
			flowData.setCommentStr(str);
			flowData.setCommentFullStr(fullString);
			//==============================
			//price
			//cell = row.getCell(8);			
			//val = cell.getNumericCellValue();
			flowData.setPrice(new Double(Math.abs(privce)).longValue());	
			//id
			cell = row.getCell(0);
			val = cell.getNumericCellValue();
			id = new Double(val).longValue();
			flowData.setId(id);			
			//uid			 
			cell = row.getCell(3); 
			val = cell.getNumericCellValue();
			uId = new Double(val).longValue();
//			if (uId == 22351510l && fullString.contains("彩票")) {
//				int a;
//				a = 1;
//			}
			flowData.setUid(uId);			
			//stamp
			cell = row.getCell(9); 
			val = cell.getNumericCellValue();
			timeStamp = new Double(val).longValue();
			flowData.setUnixStamp(timeStamp);
//			//payType
//			cell = row.getCell(9); 
//			str = cell.toString();
//			flowData.setPayType(str);
					
			//bankName
			cell = row.getCell(6);
			if (cell == null) {
				str = "xxxxxxx";
			} else {
				str = cell.toString();
			}			
			flowData.setBankNameStr(str);
			//cardid
			cell = row.getCell(4); 		
			val = cell.getNumericCellValue();					
			flowData.setCardid(new Double(val).intValue());		
			//=====
			String date = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timeStamp*1000));
			flowData.setDateStr(date);
			date = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(timeStamp*1000));
			flowData.setYearMonthDateStr(date);
			String week = getWeek(date);
			flowData.setWeekString(week);
			//
			flowData.setWeekInt(getWeekInt(week));
			cnt++;
			//add to map
			if (dataMap.containsKey(uId)) {
				ArrayList<FlowData> value = dataMap.get(uId);
				value.add(flowData);
			} else {
				ArrayList<FlowData> value = new ArrayList<FlowData>();
				value.add(flowData);
				dataMap.put(uId, value);
			}		
		}		
		System.out.println("总的纪录条数: " + end);
		System.out.println("筛选后的纪录条数: " + cnt);
		System.out.println("统计人数: " + dataMap.size());
		System.out.println("------------------------------");
		return dataMap;
	}
	
	//根据"年-月-日"返回周几
	private static String getWeek(String dateStr){
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		Date date = null;
		String[] weeks = {"星期日","星期一","星期二","星期三","星期四","星期五","星期六"};
		Calendar cal = Calendar.getInstance();
		try {
			date = format.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		};		
		cal.setTime(date);
		int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if(week_index < 0){
			week_index = 0;
		} 
		return weeks[week_index];
	}
	//返回数字类型的周几
	private static int getWeekInt(String weekStr){
		
		if (weekStr.equals("星期日")) {
			return 7;
		} else if (weekStr.equals("星期一")) {
			return 1;
		} else if (weekStr.equals("星期二")) {
			return 2;
		} else if (weekStr.equals("星期三")) {
			return 3;
		} else if (weekStr.equals("星期四")) {
			return 4;
		} else if (weekStr.equals("星期五")) {
			return 5;
		} else if (weekStr.equals("星期六")) {
			return 6;
		}
		
		return 0;
	}
	
	//返回单个人的流水记录
	public ArrayList<FlowData> getPersonFlowData(){
		return null;
	}

	public static void main(String[] args) { 
		PersonDataFromExcel var = new PersonDataFromExcel("E:/Code/java/CalcRegularityData/file/1/record1a.xlsx", "xlsx");
		int n = var.getExcelBookSheetNum2();
		
		//ArrayList<FlowData> dataList = var.readFlowDataFromSheet(0);
		long startTime=System.currentTimeMillis();
		HashMap<Long, ArrayList<FlowData>> dataMap = var.readFlowDataFromSheet2(0);
		long endTime=System.currentTimeMillis();
		System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
		
		
		//System.out.println("n = " + n + "; map size = " + dataMap.size());
		int cnt = 0;
		for (Map.Entry<Long, ArrayList<FlowData>> elem : dataMap.entrySet()) {
			long id = elem.getKey().longValue();
			ArrayList<FlowData> valueArrayList = elem.getValue();
			if (id == 1928898l) {
				int nm = valueArrayList.size();
//				for (int i = 0; i < nm; i++) {
//					System.out.println(valueArrayList.get(i).getCommentFullStr());
//				}
			}
			cnt += valueArrayList.size();
		}
		//System.out.println("sum = " + cnt);
		
	}

}
