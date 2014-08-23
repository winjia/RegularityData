package base;

import java.util.ArrayList;
import java.util.HashSet;


public class PersonalDataRecord {
	private int installmentFlg = 0; //该项纪录是否为分期付款
	private int date = 0; //非分期付款的规律日期
	private int gapDays = 0;
	private HashSet<InstallmentData> installmentDateSet = null;
	private ArrayList<FlowData> dataList = null;
	private ArrayList<Integer> ruleDateList = null;
	private double price;
	private String ruleNameStr; //规律名称
	
	public PersonalDataRecord() {
		super();
		this.date = 0;
		this.gapDays = 0;
		this.installmentFlg = 0;
		this.installmentDateSet = new HashSet<InstallmentData>();
		this.dataList = new ArrayList<FlowData>();
		this.ruleDateList = new ArrayList<Integer>();
		this.price = 0.0;
		this.ruleNameStr = null;		
	}

	public int getInstallmentFlg() {
		return installmentFlg;
	}

	public void setInstallmentFlg(int installmentFlg) {
		this.installmentFlg = installmentFlg;
	}
	
	public int getGapDays() {
		return gapDays;
	}

	public void setGapDays(int gapDays) {
		this.gapDays = gapDays;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public HashSet<InstallmentData> getInstallmentDateSet() {
		return installmentDateSet;
	}

	public void setInstallmentDateSet(HashSet<InstallmentData> installmentDateSet) {
		this.installmentDateSet = installmentDateSet;
	}

	public ArrayList<FlowData> getDataList() {
		return dataList;
	}

	public void setDataList(ArrayList<FlowData> dataList) {
		this.dataList = dataList;
	}

	public ArrayList<Integer> getRuleDateList() {
		return ruleDateList;
	}

	public void setRuleDateList(ArrayList<Integer> ruleDateList) {
		this.ruleDateList = ruleDateList;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getRuleNameStr() {
		return ruleNameStr;
	}

	public void setRuleNameStr(String ruleNameStr) {
		this.ruleNameStr = ruleNameStr;
	}		
}

