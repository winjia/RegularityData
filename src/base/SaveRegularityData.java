package base;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class SaveRegularityData {
	private HSSFWorkbook xwb = null;
	private HSSFSheet sheet = null;
	private int idx = 0;
	//
	private FileOutputStream fileOut = null;
	private OutputStreamWriter fWriter = null;
	
	public SaveRegularityData() {
		xwb = new HSSFWorkbook();
		sheet = xwb.createSheet("sheet1");
	}
	
	public SaveRegularityData(String path) {
		xwb = new HSSFWorkbook();
		sheet = xwb.createSheet("sheet1");
		//
		try {
			fileOut = new FileOutputStream(path);
			fWriter = new OutputStreamWriter(fileOut, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}
	
	public int writePersonalData2Sheet(long uid, ArrayList<PersonalDataRecord> dataList) {
		String cellStr = null;
		HSSFRow newRow = null;
		HSSFCell newCell = null;
		int nCnt = 0; //记录分期付款的数据条数
		//写入uid,第一行
		newRow = sheet.createRow(idx++);
		newCell = newRow.createCell(0);
		newCell.setCellType(HSSFCell.CELL_TYPE_STRING);
		cellStr = "UID: " +uid;
		newCell.setCellValue(cellStr);		
		//
		int n = dataList.size();
		for (int i = 0; i < n; i++) {
			PersonalDataRecord var = dataList.get(i);			
			ArrayList<FlowData> flowDataList = var.getDataList();
			int m = flowDataList.size();			
			//
			try {
				if (var.getRuleNameStr().equals("no name!")) {
					for (int j = 0; j < m; j++) {				
						FlowData flowData = flowDataList.get(j);						
						fWriter.write(flowData.getCommentFullStr()+"\n");						
					}
					fWriter.write("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
				
			//
			if (var.getInstallmentFlg() == 1) {				
				HashSet<InstallmentData> fenqiSet = var.getInstallmentDateSet();
				int mn = fenqiSet.size();
				cellStr = "共有"+mn+"个分期:";
				for (InstallmentData elem : fenqiSet) {
					int nn = elem.getPeriods()-elem.getCurrent();					
					cellStr += elem.getDate()+"日(还剩"+nn+"期,每期金额为"+elem.getPrice()+");";
				}
				//记录分期付款的条数
				nCnt += m;				
			} else {
				//cellStr = "估算日期:" + var.getDate() + "日";
				//cellStr = "平均间隔天数:" + var.getGapDays() + "天";
				//cellStr = "出现次数较多的间隔天数:" + var.getGapDays() + "天";
				cellStr = "间隔天数:" + var.getGapDays();
				/*ArrayList<Integer> dateList = var.getRuleDateList();				
				if (dateList != null) {
					String tmpString = "";
					int mn = dateList.size();
					if (mn > 0) {
						for (int j = 0; j < mn; j++) {
							tmpString += dateList.get(j)+"日; ";
						}
						cellStr += "; 规律日期:" + tmpString;
					}	
				}*/
				cellStr += "; 规律日期:"+var.getDate();
			}
			//估算日期
			newRow = sheet.createRow(idx++);
			newCell = newRow.createCell(0);
			newCell.setCellValue(cellStr);
			//
			cellStr = "";
			if (var.getInstallmentFlg() == 1) {						
				cellStr = "规律名称: "+var.getRuleNameStr();
			} else {
				cellStr = "规律名称: "+var.getRuleNameStr()+"; 金额: "+var.getPrice();
			}
			newRow = sheet.createRow(idx++);
			newCell = newRow.createCell(0);			
			newCell.setCellValue(cellStr);
			//			
			for (int j = 0; j < m; j++) {				
				FlowData flowData = flowDataList.get(j);
				newRow = sheet.createRow(idx++);
				//1.
				newCell = newRow.createCell(0);
				newCell.setCellValue(flowData.getDateStr());
				//1.1 
				newCell = newRow.createCell(1);
				newCell.setCellValue(flowData.getWeekString());
				//2.
				newCell = newRow.createCell(2);
				newCell.setCellValue(flowData.getCommentFullStr());
				//3.
				newCell = newRow.createCell(3);
				newCell.setCellValue("$"+flowData.getPrice());
				//4.
				newCell = newRow.createCell(4);
				newCell.setCellValue(flowData.getBankNameStr());
				//5.
				newCell = newRow.createCell(5);
				newCell.setCellValue(flowData.getCardid());
			}	
			idx++;
		}
		idx += 3;
		
		return nCnt;
	}
	
	
	public void save2Excel(String path){
		try {
			FileOutputStream fileOut = new FileOutputStream(path);
			xwb.write(fileOut);
		}  catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void saveNoNameText() {
		try {
			fWriter.close();
			fileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	

}
