package base;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.formula.ptg.DeletedArea3DPtg;

import com.sun.jndi.url.corbaname.corbanameURLContextFactory;
import com.sun.org.apache.regexp.internal.recompile;
import com.sun.org.apache.xml.internal.serializer.ElemDesc;

public class PersonalRegularityData {
	private int APART_DAYS = 60; //相隔天数
	private HashMap<Long, ArrayList<FlowData>> flowdataMap = null;
	private TextFilter textFilter = new TextFilter("E:/Code/java/CalcRegularityData/config");
	private HashMap<String, String> mappingMap = textFilter.getMappingMap();
	
	//---
	//返回筛选词对应的规律名称
	private String getRuleName(String commentStr) {
		ArrayList<String> keywordList = new ArrayList<String>();
		for (Map.Entry<String, String> elem : mappingMap.entrySet()) {
			String tmpStr = elem.getKey();
			String nameStr = elem.getValue();
			String keyword1 = null;
			String keyword2 = null;
			int flg = 0;
			if (tmpStr.contains("&")) {
				String[] strs = tmpStr.split("&");
				keyword1 = strs[0];
				keyword2 = strs[1];
				flg = 1;
			}
			if ((flg==1) && commentStr.contains(keyword1) && commentStr.contains(keyword2)) {
				keywordList.add(tmpStr);
			}
			else if ((flg==0) && commentStr.contains(tmpStr)){
				keywordList.add(tmpStr);
			}
		}
		int n = keywordList.size();
		int idx = -1;
		int maxer = 0;
		for (int i = 0; i < n; i++) {
			String[] strs = keywordList.get(i).split("&");
			if (maxer < strs.length) {
				idx = i;
				maxer = strs.length;
			}
		}
		if (idx == -1) {
			return "no name!";
		}
		
		return mappingMap.get(keywordList.get(idx));
	}
	public ArrayList<PersonalDataRecord> descriptionMatching(ArrayList<FlowData> dataList) {
		//variable define
		HashMap<Integer, Integer> descFreqMap = new HashMap<Integer, Integer>();
		//类似描述的下标集合
		HashMap<Integer, HashSet<Integer>> relevanceIdSetMap = new HashMap<Integer, HashSet<Integer>>();
		//根据描述进行匹配
		int n = dataList.size();
		for (int i = 0; i < n; i++) {
			int matchNum = 0;
			String str1 = dataList.get(i).getCommentStr();
			int index1 = i;
			HashSet<Integer> indexSet = new HashSet<Integer>();
			indexSet.add(index1); //加入它本身下标
			for (int j = i+1; j < n; j++) {
				String str2 = dataList.get(j).getCommentStr();
				if (str1.equals(str2)) {
					indexSet.add(j);
					matchNum++;
				}
			}
			descFreqMap.put(Integer.valueOf(index1), matchNum);
			relevanceIdSetMap.put(Integer.valueOf(index1), indexSet);
		}
		//根据id与其他id的匹配次数进行从大到小排序
		ArrayList<Map.Entry<Integer, Integer>> descFreqMapSortList = new ArrayList<Map.Entry<Integer, Integer>>(descFreqMap.entrySet());
		Collections.sort(descFreqMapSortList, new Comparator<Map.Entry<Integer, Integer>>() {
			public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});		
		//
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		int len = descFreqMapSortList.size();
		for (int i = 0; i < len; i++) {
			Integer idxInteger = descFreqMapSortList.get(i).getKey();
			indexList.add(idxInteger);
		}
		//获得根据描述聚类后的数据集合
		HashMap<Integer, ArrayList<FlowData>> clusterFlowDataMap = 
				getClusterDataListMap(dataList, indexList, relevanceIdSetMap);
		//
		ArrayList<PersonalDataRecord> finalResultList = new ArrayList<PersonalDataRecord>();
		for (Map.Entry<Integer, ArrayList<FlowData>> elem: clusterFlowDataMap.entrySet()) {
			ArrayList<FlowData> flowDatalist = elem.getValue();
			len = flowDatalist.size();
			long currentTimeStamp = 1405828800l;//System.currentTimeMillis()/1000;
			//判断时间是否符合要求
			if (!isDateNearLengthValid(currentTimeStamp, flowDatalist.get(len-1), len)) {
				continue;
			}
			String ruleNameStr = getRuleName(flowDatalist.get(0).getCommentFullStr());
			
			//判断分期付款
			if (isInstallmentData(flowDatalist)) {
				HashSet<InstallmentData> fenqiSet = installmentDataProcess(flowDatalist);
				PersonalDataRecord var1 = new PersonalDataRecord();
				var1.setInstallmentFlg(1);
				var1.setRuleNameStr(ruleNameStr);
				var1.setInstallmentDateSet(fenqiSet);
				var1.setDataList(flowDatalist);
				//var1.setPrice(price);
				finalResultList.add(var1);
				continue;
			}
			//判断是否为工资
			//boolean gongziFlg = judgeGongZi(flowDatalist);			
			//判断日期是否具有连续性
			//boolean continueFlg = isDateContinue(flowDatalist, 0.5);//0.85
			boolean isSameFlg = isSameDate(flowDatalist, 3);
			if (isSameFlg) {
				continue;
			}
			//
//			if (judgeGongZi(flowDatalist)) {
//				PersonalDataRecord var2 = new PersonalDataRecord();
//				var2.setDataList(flowDatalist);
//				finalResultList.add(var2);
//				continue;
//			}
			//
			//int nGapDays = findEqualIntervalTime(flowDatalist, flowDatalist.size()/3, 0.3);			
			//int nGapDays = findEqualIntervalTime2(flowDatalist, 2, 0.25);
			//1. 同时符合两个条件
			ArrayList<DateTimeStamp> varArrayList = deRepetitionDate(flowDatalist);
			int nGapDays = -1;
			ArrayList<Integer> dateList = null;
			nGapDays = isEqualTimeInterval(varArrayList, 0.6, 0.5);
			int ruleDate = isRegularDate(varArrayList, 5);
			//2. 符合两个条件中的一个
			if ((nGapDays!=-1) || (dateList!=null)) {
				double price = getTradeAmount(flowDatalist, 5, 0.2, 0.8);
				PersonalDataRecord var2 = new PersonalDataRecord();
				var2.setRuleNameStr(ruleNameStr);
				var2.setGapDays(nGapDays);
				//var2.setRuleDateList(dateList);
				if (ruleDate == 0) {
					ruleDate = calcTimeIntervalDate(varArrayList, nGapDays);
				}
				var2.setDate(ruleDate);
				var2.setDataList(flowDatalist);
				var2.setPrice(price);
				finalResultList.add(var2);
			}			
			//******************************************************
		}	
		//test-print
		//print(finalResultList);		
		return finalResultList;
	}
	//对"根据时间间隔得到的规律事件"进行日期的估计
	private int calcTimeIntervalDate(ArrayList<DateTimeStamp> dateTimeStampList, int nGap) {
		//计算目前的日期
		long currentTStamp = System.currentTimeMillis()/1000;
		String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(currentTStamp*1000));
		int[] ymd = getYearMonthRi(dateStr);
		int nowYear = ymd[2];
		int nowMonth = ymd[1];
		int nowDay = ymd[0];
		//如果已经存在的流水记录时间戳早于当前的时间戳则直接返回
		int n = dateTimeStampList.size();
		long prevTimeStamp = dateTimeStampList.get(n-1).getStamp(); 
		if (prevTimeStamp > currentTStamp) {
			return 0;
		}
		//如何最后一条流水的时间和目前的时间不连续,则直接返回,不进行预测
		int prevMonth = getMonth(dateTimeStampList.get(n-1).getYearMonthDateStr());
		if ((prevMonth!=12) && (Math.abs(nowMonth-prevMonth)>1)) {			
			return 0;
		} else if ((prevMonth==12) && (nowMonth>1)) {
			return 0;
		}
		//获取下个月的最后一个日期
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, nowMonth);
		int lastDate = cal.getActualMaximum(Calendar.DATE);
		//
		long newTimeStamp = prevTimeStamp + nGap*86400;
		dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(newTimeStamp*1000));
		//预测得到的日期
		int[] newYearMDVal = getYearMonthRi(dateStr); 		
		if (nowMonth == 12) {
			if ((newYearMDVal[0]<=lastDate) && (newYearMDVal[1]==1)) {
				return newYearMDVal[0];
			}
		} else {
			if ((newYearMDVal[0]<=lastDate) && ((newYearMDVal[1]-nowMonth)==1)) {
				return newYearMDVal[0];
			}
		}	
		return 0;
	}
	//计算金额是否符合波动范围内的变化
	//ratio : 波动控制
	//th : 符合范围的阈值
	//根据交易金额预测下面的金额
	//目前选6个数据
	private double getTradeAmount(ArrayList<FlowData> dataList, int requiredDays, double ratio, double th){
		int n = dataList.size();
		//(1)时间长度的有效性检查
		//--1. 如果数据长度小于要求的则直接返回
		if (n < requiredDays) {
			return 0.0;
		}
		int beg = n - requiredDays;
		//--2.获取当前的月份,并判断日期的连续性
		long currentTStamp = 1405828800l;//System.currentTimeMillis()/1000;
		String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(currentTStamp*1000));
		int month = getMonth(dateStr);
		//如果当前的month已经存在于流水记录中,则直接返回流水中的金额
		if (getMonth(dataList.get(n-1).getYearMonthDateStr()) == month) {
			return (1.0*dataList.get(n-1).getPrice());
		}
		//
		int[] gapArray = new int[n];
		int cnt = 0;
		for (int i = n-1; i >= beg; i--) {
			int monthTmp = getMonth(dataList.get(i).getYearMonthDateStr());
			int gap = 0;
			if (monthTmp==12) {
				gap = month;
			} else {
				gap = month - monthTmp;
			}
			month = monthTmp;
			gapArray[cnt++] = gap;		
		}
		//----本月前必须为连续两个月
		if ((gapArray[0]!=1) || (gapArray[1]!=1)) {
			return 0.0;
		}
		//----如果间隔大于2个月则不进行预测
		for (int i = 2; i < cnt; i++) {
			if (gapArray[i] > 2) {
				return 0.0;
			}
		}
		//(2)日期有效性间隔完毕,检测金额的有效性
		double avg = 0.0;
		long[] prices = new long[requiredDays];
		cnt = 0;
		for (int i = n-1; i >= beg; i--) {
			prices[cnt] = dataList.get(i).getPrice();
			avg += prices[cnt];
			cnt++;
		}
		avg = avg/requiredDays;
		double top = avg + ratio*avg;
		double btm = avg - ratio*avg;
		cnt = 0;
		for (int i = 0; i < requiredDays; i++) {
			if ((prices[i]>=btm) && (prices[i]<=top)) {			
				cnt++;
			}
		}
		if ((1.0*cnt/requiredDays) < th) {
			return 0.0;
		}
		return ((prices[0]+prices[1])/2.0);
	}
	
	private boolean judgeGongZi(ArrayList<FlowData> dataList) {
		int n = dataList.size();
		if (n <= 0) {
			return false;
		}
		String commentString = dataList.get(0).getCommentFullStr();
		if (commentString.contains("彩票")) {
			return true;
		}
		return false;
	}
	
	private void print(ArrayList<PersonalDataRecord> dataList){
		int n = dataList.size();
		System.out.println("length of list = " + n);
		for (int i = 0; i < n; i++) {
			PersonalDataRecord var = dataList.get(i);
			ArrayList<FlowData> flowDataList = var.getDataList();
			int m = flowDataList.size();
			if (var.getInstallmentFlg() == 1) {				
				HashSet<InstallmentData> fenqiSet = var.getInstallmentDateSet();
				int mn = fenqiSet.size();
				System.out.print("共有"+mn+"个分期:");
				for (InstallmentData elem : fenqiSet) {
					int nn = elem.getPeriods()-elem.getCurrent();
					System.out.print(elem.getDate()+"日(还剩"+nn+"期);");
				}
				System.out.println("");
			} else {
				System.out.println("估算日期:" + var.getDate() + "日");
			}
			for (int j = 0; j < m; j++) {
				System.out.println(flowDataList.get(j).getDateStr()+"---"+flowDataList.get(j).getCommentFullStr()+"---$"+flowDataList.get(j).getPrice());
			}
			System.out.println("--------------------------------");
		}		
	}
		
	//返回所需要的数据
	public HashMap<Long, ArrayList<FlowData>> getRegularityDataMap(){
		return flowdataMap;
	}
	//判断最近的日期是否距离当前时间太远
	private boolean isDateNearLengthValid(long currentTimeStamp, FlowData flowData, int len) {
		//如果数据条数过少则直接返回
		if (len <= 5) {
			return false;
		}
		//距离currentTimeStamp相距多少天
		long timeStamp = flowData.getUnixStamp();
		int nDays = getNDays(timeStamp, currentTimeStamp);
		if ((nDays>APART_DAYS) && (timeStamp<currentTimeStamp)) {
			return false;
		}			
		
		return true;
	}
		
	//判断行为发生的连续性
	private boolean isDateContinue(ArrayList<FlowData> flowDataList, double threshold){
		int n = flowDataList.size();
		ArrayList<Integer> jianGeRiQiList = new ArrayList<Integer>();
		for (int i = n-1; i >= 1; i--) {
			int days = getNDays(flowDataList.get(i).getUnixStamp(), flowDataList.get(i-1).getUnixStamp());
			jianGeRiQiList.add(Integer.valueOf(days));			
		}
		int m = jianGeRiQiList.size();
		int cnt1 = 0, cnt2 = 0, cnt3 = 0;
		double ratio1 = 0.0, ratio2 = 0.0, ratio3 = 0.0; 
		//统计符合间隔的日期数
		//1. 间隔一个月
		for (int i = 0; i < m; i++) {
			int days = jianGeRiQiList.get(i);			
			if ((days>=20) && (days<=35)) {
				cnt1++;
			}
		}		
		ratio1 = 1.0*cnt1/m;
		//2. 间隔两个月
		for (int i = 0; i < m; i++) {
			int days = jianGeRiQiList.get(i);
			if ((days>32) && (days<=65)) {
				cnt2++;
			}
		}		
		ratio2 = 1.0*cnt2/m;
		//3. 间隔三个月
		for (int i = 0; i < m; i++) {
			int days = jianGeRiQiList.get(i);
			if ((days>62) && (days<=97)) {
				cnt3++;
			}			
		}
		ratio3 = 1.0*cnt3/m;
		double maxer = 0;
		maxer = (ratio1>ratio2)?ratio1:ratio2;
		maxer = (ratio3>maxer)?ratio3:maxer;
		if (maxer > threshold) {
			return true;
		}		
		return false;
	}
	
	//寻找时间间隔差不多的事件
	//freqTh: 时间间隔出现的频率阈值
	private int findEqualIntervalTime(ArrayList<FlowData> flowDataList, int freqTh, double threshold){
		int n = flowDataList.size();
		ArrayList<Integer> jianGeRiQiList = new ArrayList<Integer>();
		for (int i = n-1; i >= 1; i--) {
			int days = getNDays(flowDataList.get(i).getUnixStamp(), flowDataList.get(i-1).getUnixStamp());
			jianGeRiQiList.add(Integer.valueOf(days));			
		}
		int m = jianGeRiQiList.size();
		//统计符合间隔的日期数
		int avgDays = 0;
		int maxDay = 0; //记录最大的时间间隔天数
		HashMap<Integer, Integer> daysGap = new HashMap<Integer, Integer>();
		for (int i = 0; i < m; i++) {
			Integer days = jianGeRiQiList.get(i);		
			avgDays += days.intValue();
			if (daysGap.containsKey(days)) {
				int value = daysGap.get(days).intValue() + 1;
				daysGap.put(days, Integer.valueOf(value));
			} else {
				daysGap.put(days, Integer.valueOf(1));
			}
			//
			if (maxDay < jianGeRiQiList.get(i)) {
				maxDay = days.intValue();
			}
		}	
		avgDays = avgDays/m;
		//判断是否有很大的时间间隔,比如说大于3个月以上
		if ((maxDay>90) && (daysGap.get(maxDay).intValue()==1)) {
			return -1;
		}
		//寻找出现频率最高的间隔天数
		int days = 0;
		int maxer = 0;
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer < elem.getValue().intValue()) {
				days = elem.getKey().intValue();
				maxer = elem.getValue().intValue();
			}
		}
		if (maxer == 1) {
			return -1;
		}
		//存放可能是符合规律的间隔天数
		ArrayList<Integer> nDaysList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer == elem.getValue().intValue()) {
				nDaysList.add(elem.getKey());
			}
		}
		//只有一个最大值
		int gap, top, bottom, cnt;
		gap = 1;
		if (nDaysList.size() == 1) {
			days = nDaysList.get(0);
			top = days - 1;
			bottom = days + 1;
			cnt = 0;
			for (int i = 0; i < m; i++) {
				days = jianGeRiQiList.get(i);
				if ((days>=top) && (days<=bottom)) {
					cnt++;
				}
			}
			if ((1.0*cnt/m) < threshold) {
				return -1;
			}
			if ((days==0) || (daysGap.get(days)<=freqTh)) {
				return -1;
			}
			return days;
		}
		//有多个最大值
		ArrayList<Integer> jianGeArrayList = new ArrayList<Integer>(); //记录合适的时间间隔
		ArrayList<Integer> ndaysList2 = new ArrayList<Integer>(); //记录出现的次数
		n = nDaysList.size();
		for (int i = 0; i < n; i++) {
			days = nDaysList.get(i);
			top = days - 1;
			bottom = days + 1;
			cnt = 0;
			for (int j = 0; j < m; j++) {
				int days2 = jianGeRiQiList.get(j);
				if ((days2>=top) && (days2<=bottom)) {
					cnt++;
				}
			}
			jianGeArrayList.add(days);
			ndaysList2.add(cnt);
		}
		int idx = 0;
		maxer = 0;
		n = ndaysList2.size();
		for (int i = 0; i < n; i++) {
			if (maxer < ndaysList2.get(i).intValue()) {
				maxer = ndaysList2.get(i).intValue();
				idx = i;
			}
		}
		if ((1.0*ndaysList2.get(idx).intValue()/m) < threshold) {
			return -1;
		}
		days = jianGeArrayList.get(idx).intValue();
		if ((days==0) || (daysGap.get(days)<=freqTh)) {
			return -1;
		}
		return days;
	}
	
	private int findEqualIntervalTime2(ArrayList<FlowData> flowDataList, int freqTh, double threshold){
		//增加"年-月"map
		int n = flowDataList.size();
		HashMap<String, ArrayList<Integer>> yearMonthMap = new HashMap<String, ArrayList<Integer>>();
		for (int i = 0; i < n; i++) {
			String fullString = flowDataList.get(i).getDateStr();
			String yearMonthVar = getYearMonth(fullString);
			int date = getDate(fullString);
			//System.out.println(yearMonthVar+"|"+fullString+"->"+date);
			if (yearMonthMap.containsKey(yearMonthVar)) {
				ArrayList<Integer> valueList = yearMonthMap.get(yearMonthVar);
				valueList.add(date);
			} else {
				ArrayList<Integer> valueList = new ArrayList<Integer>();
				valueList.add(date);
				yearMonthMap.put(yearMonthVar, valueList);
			}
		}
		//计算平均日期出现的频度
		int maxDateSize = 0;
		double avgDateNum = 0;
		for (Map.Entry<String, ArrayList<Integer>> elem : yearMonthMap.entrySet()) {
			int tmp = elem.getValue().size();
			avgDateNum += tmp;
			if (maxDateSize < tmp) {
				maxDateSize = tmp;
			}
		}
		avgDateNum /= yearMonthMap.size();
		if (maxDateSize >= 3) {
			return -1;
		}
		//		
		ArrayList<Integer> jianGeRiQiList = new ArrayList<Integer>();
		for (int i = n-1; i >= 1; i--) {
			int days = getNDays(flowDataList.get(i).getUnixStamp(), flowDataList.get(i-1).getUnixStamp());
			jianGeRiQiList.add(Integer.valueOf(days));			
		}
		int m = jianGeRiQiList.size();
		//统计符合间隔的日期数
		int avgDays = 0;
		int maxDay = 0; //记录最大的时间间隔天数
		HashMap<Integer, Integer> daysGap = new HashMap<Integer, Integer>();
		for (int i = 0; i < m; i++) {
			Integer days = jianGeRiQiList.get(i);		
			avgDays += days.intValue();
			if (daysGap.containsKey(days)) {
				int value = daysGap.get(days).intValue() + 1;
				daysGap.put(days, Integer.valueOf(value));
			} else {
				daysGap.put(days, Integer.valueOf(1));
			}
			//
			if (maxDay < jianGeRiQiList.get(i)) {
				maxDay = days.intValue();
			}
		}	
		avgDays = avgDays/m;
		//判断是否有很大的时间间隔,比如说大于3个月以上
		if ((maxDay>90) && (daysGap.get(maxDay).intValue()==1)) {
			return -1;
		}
		//寻找出现频率最高的间隔天数
		int days = 0;
		int maxer = 0;
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer < elem.getValue().intValue()) {
				days = elem.getKey().intValue();
				maxer = elem.getValue().intValue();
			}
		}
		if (maxer == 1) {
			return -1;
		}
		//存放可能是符合规律的间隔天数
		ArrayList<Integer> nDaysList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer == elem.getValue().intValue()) {
				nDaysList.add(elem.getKey());
			}
		}
		//只有一个最大值
		int gap, top, bottom, cnt;
		gap = 2;
		if (nDaysList.size() == 1) {
			days = nDaysList.get(0);
			top = days - gap;
			bottom = days + gap;
			cnt = 0;
			for (int i = 0; i < m; i++) {
				int days2 = jianGeRiQiList.get(i);
				if ((days2>=top) && (days2<=bottom)) {
					cnt++;
				}
			}
			if ((1.0*cnt/m) < threshold) {
				return -1;
			}
			if ((days==0) || (daysGap.get(days)<=freqTh)) {
				return -1;
			}
			return days;
		}
		//有多个最大值
		ArrayList<Integer> jianGeArrayList = new ArrayList<Integer>(); //记录合适的时间间隔
		ArrayList<Integer> ndaysList2 = new ArrayList<Integer>(); //记录出现的次数
		n = nDaysList.size();
		for (int i = 0; i < n; i++) {
			days = nDaysList.get(i); //间隔天数
			top = days - gap;
			bottom = days + gap;
			cnt = 0;
			for (int j = 0; j < m; j++) {
				int days2 = jianGeRiQiList.get(j);
				if ((days2>=top) && (days2<=bottom)) {
					cnt++;
				}
			}
			jianGeArrayList.add(days);
			ndaysList2.add(cnt);
		}
		int idx = 0;
		maxer = 0;
		n = ndaysList2.size();
		for (int i = 0; i < n; i++) {
			if (maxer < ndaysList2.get(i).intValue()) {
				maxer = ndaysList2.get(i).intValue();
				idx = i;
			}
		}		
		//统计出现最大值的时间间隔
		int sum1 = 0, sum2 = 0;
		for (int i = 0; i < n; i++) {
			if (ndaysList2.get(i).intValue() == maxer) {
				Integer tmp = jianGeArrayList.get(i);
				sum1 += daysGap.get(tmp).intValue(); //间隔天数的累加
			}
		}
		if ((1.0*ndaysList2.get(idx).intValue()/m) < threshold) {
			return -1;
		}
		
		//if ((days==0) || (daysGap.get(days)<=freqTh)) {
		if ((days==0) || (sum1<=freqTh)) {
			return -1;
		}
		return days;
	}	
	
	//***过滤多次出现的相同日期放***
	private ArrayList<DateTimeStamp> deRepetitionDate(ArrayList<FlowData> flowDataList){
		//去重: 相同日期的项只记一次
		int n = flowDataList.size();		
		DateTimeStamp[] dateArray = new DateTimeStamp[n];  
		int idx = 0;
		for (int i = 0; i < n; i++) {
			FlowData flowData = flowDataList.get(i);
			String dateString = flowData.getDateStr();
			String yearMDStr = getYearMonthRiStr(dateString);			 
			long timeStamp = flowData.getUnixStamp();
			int weekInt = flowData.getWeekInt();
			int dateVal = getDate(dateString);
			if (i == 0) {
				DateTimeStamp var = new DateTimeStamp(yearMDStr, timeStamp, weekInt, dateVal);
				dateArray[idx] = var;
				idx++;
				continue;
			}
			if (dateArray[idx-1].getYearMonthDateStr().equals(yearMDStr)) {
				continue;
			}
			DateTimeStamp var = new DateTimeStamp(yearMDStr, timeStamp, weekInt, dateVal);
			dateArray[idx] = var;
			idx++;
		}
		ArrayList<DateTimeStamp> dateTimeStampList = new ArrayList<DateTimeStamp>();
		for (int i = 0; i < idx; i++) {
			dateTimeStampList.add(dateArray[i]);
		}
		return dateTimeStampList;
	}
	//***时间间隔的合理性***
	private int isEqualTimeInterval(ArrayList<DateTimeStamp> dateTimeStampList, double topTh, double ratio) {
		//计算时间间隔gap,按顺序:远->近
		ArrayList<Integer> timeIntervalList = new ArrayList<Integer>();
		int n = dateTimeStampList.size();
		for (int i = 1; i < n; i++) {
			int days = getNDays(dateTimeStampList.get(i-1).getStamp(), dateTimeStampList.get(i).getStamp());
			timeIntervalList.add(days);
		}
		//统计时间间隔gap的出现次数
		HashMap<Integer, Integer> gapFreqMap = new HashMap<Integer, Integer>();
		n = timeIntervalList.size();
		for (int i = 0; i < n; i++) {
			Integer keyInteger = timeIntervalList.get(i);
			if (gapFreqMap.containsKey(keyInteger)) {
				int value = gapFreqMap.get(keyInteger).intValue() + 1;
				gapFreqMap.put(keyInteger, value);
			} else {
				gapFreqMap.put(keyInteger, Integer.valueOf(1));
			}
		}
		//判断间隔出现的合理性
		int maxCnt = 0; //出现次数最多的时间间隔
		int gaps = 0;
		for (Map.Entry<Integer, Integer> elem : gapFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
				gaps = elem.getKey().intValue();
			}
		}
		//如果最大的间隔出现次数==1或者间隔数为1,则认为发生太频繁,给予滤除
		if ((maxCnt==1) || (gaps==0) || (gaps==1)) {
			return -1;
		}
		//如果出现次数最多的时间间隔占有的比例大于topTh,则认为是
		//对选到的gap进行扩展以判断是否为规律时间间隔
		int top = gaps + 1;
		int btm = gaps - 1;
		maxCnt = 0;
		for (int i = 0; i < n; i++) {
			int d = timeIntervalList.get(i).intValue();
			if ((d>=btm) && (d<=top)) {
				maxCnt++;
			}
		}
		if ((1.0*maxCnt/n) > topTh) {
			return gaps;
		}
		//如果占有比例不够,则按照由近到远的时间顺序,判断时间间隔的合理性
		//选取近距离的50%
		btm = (int)(n*ratio);
		//如果取半后的数据条数条数过少,则
		if ((n-btm+1) <= 5) {
			return -1;
		}
		ArrayList<Integer> timeIntervalList2 = new ArrayList<Integer>();
		for (int i = btm+1; i < n; i++) {
			int d = timeIntervalList.get(i-1).intValue() - timeIntervalList.get(i).intValue();
			d = Math.abs(d);
			timeIntervalList2.add(d);
		}
		n = timeIntervalList2.size();
//		if (n < 4) {
//			return -1;
//		}
		for (int i = 0; i < n; i++) {
			if (timeIntervalList2.get(i).intValue() > 1) {
				return -1;
			}
		}
		n = timeIntervalList.size();
		return timeIntervalList.get(n-1).intValue();
	}
	
	//***规律日期的选择***
	private int isRegularDate(ArrayList<DateTimeStamp> dateTimeStampList, int nEffectiveDays) {
		//统计"日"的出现频率
		int n = dateTimeStampList.size();
		HashMap<Integer, Integer> dateFreqMap = new HashMap<Integer, Integer>();
		ArrayList<Integer> dateList = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			String yMDString = dateTimeStampList.get(i).getYearMonthDateStr();
			String[] splitStrs = yMDString.split("/");
			String dateString = splitStrs[0];
			Integer dateInteger = Integer.valueOf(dateString);
			dateList.add(dateInteger);
			if (dateFreqMap.containsKey(dateInteger)) {
				int value = dateFreqMap.get(dateInteger).intValue() + 1;
				dateFreqMap.put(dateInteger, Integer.valueOf(value));
			} else {
				dateFreqMap.put(dateInteger, Integer.valueOf(1));
			}
		}
		//寻找最大的日期出现次数
		int maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
			}
		}
		//如果出现次数最大为1
		if (maxCnt == 1) {
			return 0;
		}
		//将出现次数==maxCnt的date存起来
		ArrayList<Integer> maxFreqList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				maxFreqList.add(elem.getKey());
			}
		}
		//从候选日期中找到最佳日期
		n = maxFreqList.size();
		int m = dateList.size();
		dateFreqMap.clear();
		for (int i = 0; i < n; i++) {
			int d = maxFreqList.get(i).intValue();
			int beg = d-2;
			int end = d+2;
			beg = (beg<1)?1:beg;
			end = (end>30)?31:end;
			int cnt = 0;
			for (int j = 0; j < m; j++) {
				int tmp = dateList.get(j).intValue();
				if ((tmp>=beg) && (tmp<=end)) {
					cnt++;
				}
			}
			dateFreqMap.put(Integer.valueOf(d), Integer.valueOf(cnt));					
		}
		maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
			}
		}
		if ((1.0*maxCnt/m) < 0.7) {
			return 0;
		}
		//将出现频率一样的日期保存下来
		ArrayList<Integer> resDateList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				resDateList.add(elem.getKey());
			}
		}
		//选择近期出现频率较高的日期
		//选择的统计数据长度为nEffectiveDays
		m = dateTimeStampList.size();
		if (m < nEffectiveDays) {
			return dateTimeStampList.get(m-1).getDateVal();
		}
		n = m - nEffectiveDays;
		dateFreqMap.clear();
		//统计后nEffectiveDays段时间内出现的日期的频率dateFreqMap
		for (int i = m-1; i >= n; i--) {
			String yMDString = dateTimeStampList.get(i).getYearMonthDateStr();
			String[] splitStrs = yMDString.split("/");
			String dateString = splitStrs[0];
			Integer dateInteger = Integer.valueOf(dateString);
			dateList.add(dateInteger);
			if (dateFreqMap.containsKey(dateInteger)) {
				int value = dateFreqMap.get(dateInteger).intValue() + 1;
				dateFreqMap.put(dateInteger, Integer.valueOf(value));
			} else {
				dateFreqMap.put(dateInteger, Integer.valueOf(1));
			}			
		}
		maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
			}
		}
		//出现频率最大的日期的候选列表maxFreqList
		maxFreqList.clear();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				maxFreqList.add(elem.getKey());
			}
		}
		//如果只出现一个日期,则判断该日期是否出现在统计列表resDateList中,如果存在则
		if (maxFreqList.size() == 1) {
			Integer dateTmp = maxFreqList.get(0);
			for (Integer integer : resDateList) {
				if (integer.equals(dateTmp)) {
					return dateTmp.intValue();
				}
			}
			return dateTimeStampList.get(m-1).getDateVal();
		} 
		//出现多个日期
		return dateTimeStampList.get(m-1).getDateVal();		
	}
	
	
	//***根据星期规律来判断***
	private HashSet<Integer> isRegularWeek(ArrayList<DateTimeStamp> dateTimeStampList){
		int n = dateTimeStampList.size();
		HashMap<Integer, Integer> weekMap = new HashMap<Integer, Integer>();
		HashSet<Integer> weekSet = new HashSet<Integer>();
		//统计星期出现的次数
		for (int i = 0; i < n; i++) {
			Integer varInteger = Integer.valueOf(dateTimeStampList.get(i).getWeek());
			weekSet.add(varInteger);
			if (weekMap.containsKey(varInteger)) {
				int val = weekMap.get(varInteger).intValue() + 1;
				weekMap.put(varInteger, Integer.valueOf(val));
			} else {
				weekMap.put(varInteger, Integer.valueOf(1));
			}
		}
		//如果每个var的出现次数和平均次数差距不大的时候,则认为是有规律星期
		boolean flg = true;
		int mapSize = weekMap.size();
		int avg = n/mapSize;
		for (Map.Entry<Integer, Integer> elem : weekMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (Math.abs(tmp-avg) > 1) {
				flg = false;
			}
		}
		if (flg) {
			return weekSet;
		}
		//如果"星期"没有符合前一个特征,则进行接下来的判断
		int beg = (int)(0.5*n);
		int end = n;
		if ((end-beg+1) < 6) {
			return null;
		}
		for (int i = 0; i < end; i++) {
			
		}
		return weekSet;
	}
	
	//获取归类后的数据记录
	public HashMap<Integer, ArrayList<FlowData>> getClusterDataListMap(ArrayList<FlowData> dataList, ArrayList<Integer> descFreqMapSortList, HashMap<Integer, HashSet<Integer>> relevanceIdSetMap) {
		HashSet<Integer> idFlgSet = new HashSet<Integer>(); //记录用过的id
		HashMap<Integer, ArrayList<FlowData>> clusterFlowDataMap = new HashMap<Integer, ArrayList<FlowData>>();
		int n = descFreqMapSortList.size();
		for (int i = 0; i < n; i++) {
			Integer idx = descFreqMapSortList.get(i);
			//如果该id已经存在,则排除
			if (idFlgSet.contains(idx)) {
				continue;
			}
			//如果列表长度小于3则排除
			HashSet<Integer> idxSet = relevanceIdSetMap.get(idx);
			if (idxSet.size() <= 3) {
				idFlgSet.add(idx);
				continue;
			}
			//根据index列表,建立新的列表
			ArrayList<FlowData> tmpFlowDataList = new ArrayList<FlowData>();
			for (Integer id : idxSet) {
				tmpFlowDataList.add(dataList.get(id.intValue()));
				idFlgSet.add(id);
			}	
			sortFlowDataByUnixStamp(tmpFlowDataList);
			clusterFlowDataMap.put(idx, tmpFlowDataList);			
		}
		
		return clusterFlowDataMap;
	}
	
	//价格判断
	//返回符合条件的比率
	//threshold : 如果数据条数小余阈值,则不符合规则
	//weight
	private double calcRegularityPrice(ArrayList<FlowData> flowDataList, int threshold, double weight) {
		
		int n = flowDataList.size();
		if (n < threshold) {
			return 0.0;
		}
		ArrayList<Double> valList = new ArrayList<Double>();
		double meanVal = 0.0;
		double stdVal = 0.0;
		for (int i = 0; i < n; i++) {
			double tmp = Math.abs(flowDataList.get(i).getPrice()/100.0);
			valList.add(Double.valueOf(tmp));
			meanVal += tmp;
		}
		meanVal = meanVal/n;
		for (int i = 0; i < n; i++) {
			stdVal += (valList.get(i)-meanVal)*(valList.get(i)-meanVal);
		}
		stdVal = Math.sqrt(stdVal/n);
		//
		double downLimit = meanVal - weight*stdVal;
		double topLimit = meanVal + weight*stdVal;
		int cnt = 0;
		for (int i = 0; i < n; i++) {
			if ((valList.get(i)>=downLimit) && (valList.get(i)<=topLimit)) {
				cnt++;
			}
		}		
		return (cnt*1.0/n);
	}
	
	//根据时间戳进行排序
	private void sortFlowDataByUnixStamp(ArrayList<FlowData> dataList){
		Collections.sort(dataList, new Comparator<FlowData>() {			
			public int compare(FlowData var1, FlowData var2) {
				long a1 = var1.getUnixStamp();
				long a2 = var2.getUnixStamp();				
				if (a1 < a2) {
					return -1;
				}
				else if (a1 ==a2) {
					return 0;
				}
				return 1;
			}				
		});	
	}

	//判断是否为分期付款数据
	private boolean isInstallmentData(ArrayList<FlowData> flowDataList) {
		//regex
		Pattern pattern1 = Pattern.compile("[0-9]{2}-[0-9]{2}");
		Pattern pattern2 = Pattern.compile("[0-9]{2}/[0-9]{2}");		
		//判断是否为"区号-电话号码"格式
		Pattern pattern3 = Pattern.compile("[0-9]{3}-[0-9]{4}");
		//判断是否为日期形式
		Pattern pattern4 = Pattern.compile("[0-9]{2}-[0-9]{2}-[0-9]{2}");
		//
		Pattern pattern5 = Pattern.compile("[0-9]{2}/[0-9]{3}");
		//"第1期共3期"
		Pattern pattern6 = Pattern.compile("第[0-9]期共[0-9]期");
		//
		int flg = 0;
		int n = flowDataList.size();
		for (int i = 0; i < n; i++) {
			String descString = flowDataList.get(i).getCommentFullStr();			
			Matcher matcher1 = pattern1.matcher(descString);
			Matcher matcher2 = pattern2.matcher(descString);
			Matcher matcher3 = pattern3.matcher(descString);
			Matcher matcher4 = pattern4.matcher(descString);
			Matcher matcher5 = pattern5.matcher(descString);
			Matcher matcher6 = pattern6.matcher(descString);
			if (matcher3.find() || matcher4.find() || matcher5.find()) {
				flg = 1;
				break;
			}
			if ((matcher2.find()==false) && (matcher1.find()==false) && (matcher6.find()==false)) {				
				flg = 1;
				break;
			}	
			String leftString = null;
			String rightString = null;
			if (matcher1.find()) {
				String string = matcher1.group();
				String[] strs = string.split("-");
				leftString = strs[0];
				rightString = strs[1];		
			} else if (matcher2.find()) {
				String string = matcher1.group();
				String[] strs = string.split("/");
				leftString = strs[0];
				rightString = strs[1];
			}
			if ((leftString=="01") && (rightString=="01")) {
				flg = 1;
				break;
			}
			
		}
		//非分期付款
		if (flg == 1) {
			return false;
		}
		return true;
	}
	//解析分期付款数据
	private HashSet<InstallmentData> installmentDataProcess(ArrayList<FlowData> flowDataList){
		Pattern pattern1 = Pattern.compile("[0-9]{2}-[0-9]{2}");
		Pattern pattern2 = Pattern.compile("[0-9]{2}/[0-9]{2}");
		Pattern pattern3 = Pattern.compile("第[0-9]期共[0-9]期");
		HashSet<InstallmentData> fenqiSet = new HashSet<InstallmentData>();
		ArrayList<InstallmentData> fenqiList = new ArrayList<InstallmentData>();
		int n = flowDataList.size();
		String string = null;
		for (int i = 0; i < n; i++) {
			long price = flowDataList.get(i).getPrice();
			String dateString = flowDataList.get(i).getDateStr();
			String descString = flowDataList.get(i).getCommentFullStr();
			int date = getDate(dateString);
			Matcher matcher1 = pattern1.matcher(descString);
			Matcher matcher2 = pattern2.matcher(descString);
			Matcher matcher3 = pattern3.matcher(descString);
			//
			if (matcher1.find()) {				
				string = matcher1.group();
				String[] strs = string.split("-");
				int current = Integer.valueOf(strs[0]).intValue();
				int pred = Integer.valueOf(strs[1]).intValue();
				InstallmentData var = new InstallmentData(pred, price, (int)(price/100), current, date);
				fenqiSet.add(var);
				fenqiList.add(var);
			} else if (matcher2.find()) {				
				string = matcher2.group();
				String[] strs = string.split("/");
				int current = Integer.valueOf(strs[0]).intValue();
				int pred = Integer.valueOf(strs[1]).intValue();				
				InstallmentData var = new InstallmentData(pred, price, (int)(price/100), current, date);
				fenqiSet.add(var);
				fenqiList.add(var);
			} else if (matcher3.find()) {				
				string = matcher3.group();
				String val1 = string.substring(1, 2);
				String val2 = string.substring(4, 5);				
				int current = Integer.valueOf(val1).intValue();
				int pred = Integer.valueOf(val2).intValue();				
				InstallmentData var = new InstallmentData(pred, price, (int)(price/100), current, date);
				fenqiSet.add(var);
				fenqiList.add(var);
			}
		}
		//
		n = fenqiList.size();
		for (InstallmentData elem : fenqiSet) {
			for (int i = n-1; i >= 0; i--) {
				if (fenqiList.get(i).equals(elem)) {
					elem.setCurrent(fenqiList.get(i).getCurrent());
					break;
				}
			}
		}
		
		return fenqiSet;
	}
	
	//获取估计得到的日期
	private int getRegularityDate(ArrayList<FlowData> flowDataList, double threshold) {
		int n = flowDataList.size();
		HashMap<Integer, Integer> dateCntMap = new HashMap<Integer, Integer>();
		ArrayList<Integer> allDateList = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			Integer dateInt = getDate(flowDataList.get(i).getDateStr());
			allDateList.add(dateInt);
			if (dateCntMap.containsKey(dateInt)) {
				int valueInteger = dateCntMap.get(dateInt).intValue() + 1;
				dateCntMap.put(dateInt, Integer.valueOf(valueInteger));
			} else {
				dateCntMap.put(dateInt, new Integer(1));
			}
		}
		//寻找出现次数最多的日期
		int maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateCntMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (tmp > maxCnt) {
				maxCnt = tmp;
			}
		}
		//可能是有规律日期的列表
		ArrayList<Integer> dateList = new ArrayList<Integer>(); 
		for (Map.Entry<Integer, Integer> elem : dateCntMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (tmp == maxCnt) {
				dateList.add(elem.getKey());
			}
		}		
		int gap = 2;
		//如果只有一个唯一日期就立即返回
		if (dateList.size() == 1) {
			int date = dateList.get(0).intValue();
			int top = (date+gap)>=30?31:(date+gap);
			int down = (date-gap)<=0?1:(date-gap);
			maxCnt = 0;
			for (int i = 0; i < n; i++) {
				int dateInt = allDateList.get(i).intValue();
				if ((dateInt>=down) && (dateInt<=top)) {
					maxCnt++;
				}
			}
			if ((1.0*maxCnt/n) < threshold) {
				return 0;
			}
			return dateList.get(0).intValue();
		}
		//对于多个日期进行再次判断
		int m = dateList.size();
		ArrayList<Integer> dateCntList = new ArrayList<Integer>();
		for (int i = 0; i < m; i++) {
			int date = dateList.get(i).intValue();
			int top = (date+gap)>=30?31:(date+gap);
			int down = (date-gap)<=0?1:(date-gap);
			int c = 0;
			for (int j = 0; j < n; j++) {
				int dateInt = allDateList.get(j).intValue();
				if ((dateInt>=down) && (dateInt<=top)) {
					c++;
				}
			}
			dateCntList.add(Integer.valueOf(c));
		}
		maxCnt = 0;
		int idx = 0;
		for (int i = 0; i < m; i++) {
			if (maxCnt < dateCntList.get(i)) {
				maxCnt = dateCntList.get(i);
				idx = i;
			}
		}
		if ((1.0*maxCnt/n) < threshold) {
			return 0;
		}
		return dateList.get(idx).intValue();
	}
	
	//判断: 如果一个月内出现多次付款记录则认为是非规律数据
	//1.比如说多次出现相同的时间: 01/05/2014
	//2.或者:01/05/2014, 02/05/2014, 03/05/2014
	private boolean isRegularityDate(ArrayList<FlowData> flowDataList){
		int n = flowDataList.size();
		HashMap<String, Integer> dateCntMap = new HashMap<String, Integer>();
		for (int i = 0; i < n; i++) {
			String dateString = flowDataList.get(i).getDateStr();
			//String yueriStr = getMonthRi(dateString);
			String yueriStr = getYearMonth(dateString);
			if (dateCntMap.containsKey(yueriStr)) {
				int val = dateCntMap.get(yueriStr).intValue() + 1;
				dateCntMap.put(yueriStr, Integer.valueOf(val));
			} else {
				dateCntMap.put(yueriStr, Integer.valueOf(1));
			}
		}	
		for (Map.Entry<String, Integer> elem : dateCntMap.entrySet()) {
			if (elem.getValue().intValue() >= 3) {
				return false;
			}
		}		
		return true;
	}
	//判断是否
	private boolean isSameDate(ArrayList<FlowData> flowDataList, int threshold){
		int n = flowDataList.size();
		HashMap<String, Integer> dateCntMap = new HashMap<String, Integer>();
		for (int i = 0; i < n; i++) {
			String dateString = flowDataList.get(i).getDateStr();
			String yueriStr = getMonthRi(dateString);
			if (dateCntMap.containsKey(yueriStr)) {
				int val = dateCntMap.get(yueriStr).intValue() + 1;
				dateCntMap.put(yueriStr, Integer.valueOf(val));
			} else {
				dateCntMap.put(yueriStr, Integer.valueOf(1));
			}
		}	
		for (Map.Entry<String, Integer> elem : dateCntMap.entrySet()) {
			if (elem.getValue().intValue() >= threshold) {
				return true;
			}
		}		
		return false;
	}
	
	//-----------------------------------------------------
	//输入两个时间戳,返回相差的天数
	private int getNDays(long ts1, long ts2) {
		long nSec = 0;
		if (ts1 > ts2) {
			nSec = ts1 - ts2;
		} else {
			nSec = ts2 - ts1;
		}
		double days = nSec/(3600.0*24);
		
		return ((int)(days+0.5));		
	}
	//返回“y/m/d”中的d
	private int getDate(String dateStr){
		//String string = "24/03/2014 07:05:42";
		String[] strs = dateStr.split("/");
		//System.out.println("asfdad = "+strs.length+":"+strs[0]);
		if (strs.length <= 1) {
			return 0;
		}
		return Integer.valueOf(strs[0]).intValue();
	}
	//返回"月"
	private int getMonth(String dateStr){
		String[] strs = dateStr.split("/");
		if (strs.length <= 1) {
			return 0;
		}
		return Integer.valueOf(strs[1]).intValue();
		
	}
	//返回"日-月-年"整型
	private int[] getYearMonthRi(String timeString) {
		String[] strs = timeString.split("/");
		if (strs.length < 3) {
			return null;
		}
		int[] ymd = new int[3];
		ymd[0] = Integer.valueOf(strs[0]).intValue(); //date
		ymd[1] = Integer.valueOf(strs[1]).intValue(); //month
		ymd[2] = Integer.valueOf(strs[2]).intValue(); //year
		return ymd;
	}
	//返回"日-月-年"整型
	private String getYearMonthRiStr(String timeString) {
		String[] strs = timeString.split(" ");		
		if (strs.length <= 1) {
			return null;
		}   
		return strs[0];
	}
	//返回"月-年"字符串
	private String getYearMonth(String timeString) {
		String[] strs = timeString.split(" ");
		if (strs.length <= 1) {
			return null;
		}
		strs = strs[0].split("/");
		if (strs.length <= 1) {
			return null;
		}
		String tmp = strs[1] + "-" + strs[2];
		return tmp;
	}
	//返回"日-月"字符串
	private String getMonthRi(String timeString) {
		String[] strs = timeString.split(" ");
		if (strs.length <= 1) {
			return null;
		}
		strs = strs[0].split("/");
		if (strs.length <= 1) {
			return null;
		}
		String tmp = strs[0] + "-" + strs[1];
		return tmp;
	}

}
