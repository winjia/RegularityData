package base;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MainProcess {
	private String inPath = null;
	private String outPath = null;
	private PersonalRegularityData regularityData = new PersonalRegularityData();
	private PersonDataFromExcel personData = null;
	private SaveRegularityData saveHandle = null;
	//
	private int totalPeopleNum = 0;
	private int totalDataNum = 0;
	private int haveRecordCnt = 0;
	private int noRecordPersonCnt = 0;
	private int regularityDataCnt = 0;
	private int installmentNum = 0;
	
	public MainProcess() {
		personData = new PersonDataFromExcel("E:/Code/java/CalcRegularityData/file/1.xls", "xls");
		this.outPath = "E:/Code/java/CalcRegularityData/file/allresult_100.xls";		
	}
	
	public MainProcess(String inPath, String outPath) {
		//personData = new PersonDataFromExcel(inPath, "xls");
		personData = new PersonDataFromExcel(inPath, "xlsx");
		this.outPath = outPath;
		//
		String[] path = outPath.split("\\.");
		saveHandle = new SaveRegularityData(path[0]+".txt");
	}	
	
	public void Process() {
		long startTime = 0l, endTime = 0l;
		//
		startTime = System.currentTimeMillis();
		//HashMap<Long, ArrayList<FlowData>> dataMap = personData.readFlowDataFromSheet(0);
		HashMap<Long, ArrayList<FlowData>> dataMap = personData.readFlowDataFromSheet2(0);
		endTime = System.currentTimeMillis();
		System.out.println("Read Excel file elapse time = "+(endTime-startTime)+"ms");
		totalPeopleNum = dataMap.size();
		startTime = System.currentTimeMillis();
		for (Map.Entry<Long, ArrayList<FlowData>> elem : dataMap.entrySet()) {
			Long uid = elem.getKey();
			ArrayList<FlowData> valueList = elem.getValue();
			totalDataNum += valueList.size();
			if (uid == 21986677l) {
				int a = 0;
				a = 1;
			}
			ArrayList<PersonalDataRecord> res = regularityData.descriptionMatching(valueList);
			if (res.size() == 0) {
				noRecordPersonCnt++;
			} else {
				regularityDataCnt += regularityDataNum(res);
				installmentNum += saveHandle.writePersonalData2Sheet(uid, res);
			}
		}
		endTime = System.currentTimeMillis();
		System.out.println("Process data elapse time = "+(endTime-startTime)+"ms");
		//
		//saveHandle.save2Excel(outPath);
		saveHandle.saveNoNameText();
		//
		haveRecordCnt = (totalPeopleNum-noRecordPersonCnt);
		System.out.println("总共人数: "+totalPeopleNum);
		System.out.println("总的待处理记录条数:" + totalDataNum);
		System.out.println("具有规律的人数:" + haveRecordCnt);
		System.out.println("规律数据条数:" + regularityDataCnt);
		System.out.println("分期数据条数:"+installmentNum);
		System.out.println("具有规律数据的人数占总人数的比例:"+(1.0*haveRecordCnt/totalPeopleNum));
		System.out.println("----------------------------------------------");
	}
	
	private int regularityDataNum(ArrayList<PersonalDataRecord> dataList){
		int n = dataList.size();
		int cnt = 0;
		for (int i = 0; i < n; i++) {
			ArrayList<FlowData> var = dataList.get(i).getDataList();
			cnt += var.size();
		}
		return cnt;
	}
	
	public static void main(String[] args) {
		//MainProcess var = new MainProcess();
		MainProcess var = new MainProcess("E:/Code/java/CalcRegularityData/file/3/r5.xlsx",
				"E:/Code/java/CalcRegularityData/file/3/res_r5.xls");
		long startTime=System.currentTimeMillis();
		var.Process();
		long endTime=System.currentTimeMillis();
		System.out.println("Elapse time = "+(endTime-startTime)+"ms");
		
		
//		OutputStream ostream = null;		
//		try {
//			OPCPackage pkg = OPCPackage.open("E:/Code/java/CalcRegularityData/file/record.xlsx");
//			XSSFWorkbook xwb;
//			try {
//				xwb = new XSSFWorkbook(pkg);
//				XSSFSheet sheet = xwb.getSheetAt(0);
//				System.out.println(sheet.getFirstRowNum());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		} catch (InvalidFormatException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
//		String path = "E:/Code/java/CalcRegularityData/file/record1.xlsx";
//		FileInputStream input;
//		try {
//			//input = new FileInputStream(new File("E:/Code/java/CalcRegularityData/file/record.xlsx"));
//			OPCPackage pkg = OPCPackage.open(path);
//			XSSFWorkbook xwb = new XSSFWorkbook(pkg);
//			XSSFSheet sheet = xwb.getSheetAt(0);
//			System.out.println(sheet.getFirstRowNum());
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}  
//		//SXSSFWorkbook
//		catch (InvalidFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
		

	}
}
