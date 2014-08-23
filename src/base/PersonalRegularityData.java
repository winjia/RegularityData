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
	private int APART_DAYS = 60; //�������
	private HashMap<Long, ArrayList<FlowData>> flowdataMap = null;
	private TextFilter textFilter = new TextFilter("E:/Code/java/CalcRegularityData/config");
	private HashMap<String, String> mappingMap = textFilter.getMappingMap();
	
	//---
	//����ɸѡ�ʶ�Ӧ�Ĺ�������
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
		//�����������±꼯��
		HashMap<Integer, HashSet<Integer>> relevanceIdSetMap = new HashMap<Integer, HashSet<Integer>>();
		//������������ƥ��
		int n = dataList.size();
		for (int i = 0; i < n; i++) {
			int matchNum = 0;
			String str1 = dataList.get(i).getCommentStr();
			int index1 = i;
			HashSet<Integer> indexSet = new HashSet<Integer>();
			indexSet.add(index1); //�����������±�
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
		//����id������id��ƥ��������дӴ�С����
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
		//��ø����������������ݼ���
		HashMap<Integer, ArrayList<FlowData>> clusterFlowDataMap = 
				getClusterDataListMap(dataList, indexList, relevanceIdSetMap);
		//
		ArrayList<PersonalDataRecord> finalResultList = new ArrayList<PersonalDataRecord>();
		for (Map.Entry<Integer, ArrayList<FlowData>> elem: clusterFlowDataMap.entrySet()) {
			ArrayList<FlowData> flowDatalist = elem.getValue();
			len = flowDatalist.size();
			long currentTimeStamp = 1405828800l;//System.currentTimeMillis()/1000;
			//�ж�ʱ���Ƿ����Ҫ��
			if (!isDateNearLengthValid(currentTimeStamp, flowDatalist.get(len-1), len)) {
				continue;
			}
			String ruleNameStr = getRuleName(flowDatalist.get(0).getCommentFullStr());
			
			//�жϷ��ڸ���
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
			//�ж��Ƿ�Ϊ����
			//boolean gongziFlg = judgeGongZi(flowDatalist);			
			//�ж������Ƿ����������
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
			//1. ͬʱ������������
			ArrayList<DateTimeStamp> varArrayList = deRepetitionDate(flowDatalist);
			int nGapDays = -1;
			ArrayList<Integer> dateList = null;
			nGapDays = isEqualTimeInterval(varArrayList, 0.6, 0.5);
			int ruleDate = isRegularDate(varArrayList, 5);
			//2. �������������е�һ��
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
	//��"����ʱ�����õ��Ĺ����¼�"�������ڵĹ���
	private int calcTimeIntervalDate(ArrayList<DateTimeStamp> dateTimeStampList, int nGap) {
		//����Ŀǰ������
		long currentTStamp = System.currentTimeMillis()/1000;
		String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(currentTStamp*1000));
		int[] ymd = getYearMonthRi(dateStr);
		int nowYear = ymd[2];
		int nowMonth = ymd[1];
		int nowDay = ymd[0];
		//����Ѿ����ڵ���ˮ��¼ʱ������ڵ�ǰ��ʱ�����ֱ�ӷ���
		int n = dateTimeStampList.size();
		long prevTimeStamp = dateTimeStampList.get(n-1).getStamp(); 
		if (prevTimeStamp > currentTStamp) {
			return 0;
		}
		//������һ����ˮ��ʱ���Ŀǰ��ʱ�䲻����,��ֱ�ӷ���,������Ԥ��
		int prevMonth = getMonth(dateTimeStampList.get(n-1).getYearMonthDateStr());
		if ((prevMonth!=12) && (Math.abs(nowMonth-prevMonth)>1)) {			
			return 0;
		} else if ((prevMonth==12) && (nowMonth>1)) {
			return 0;
		}
		//��ȡ�¸��µ����һ������
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 2014);
		cal.set(Calendar.MONTH, nowMonth);
		int lastDate = cal.getActualMaximum(Calendar.DATE);
		//
		long newTimeStamp = prevTimeStamp + nGap*86400;
		dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(newTimeStamp*1000));
		//Ԥ��õ�������
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
	//�������Ƿ���ϲ�����Χ�ڵı仯
	//ratio : ��������
	//th : ���Ϸ�Χ����ֵ
	//���ݽ��׽��Ԥ������Ľ��
	//Ŀǰѡ6������
	private double getTradeAmount(ArrayList<FlowData> dataList, int requiredDays, double ratio, double th){
		int n = dataList.size();
		//(1)ʱ�䳤�ȵ���Ч�Լ��
		//--1. ������ݳ���С��Ҫ�����ֱ�ӷ���
		if (n < requiredDays) {
			return 0.0;
		}
		int beg = n - requiredDays;
		//--2.��ȡ��ǰ���·�,���ж����ڵ�������
		long currentTStamp = 1405828800l;//System.currentTimeMillis()/1000;
		String dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(currentTStamp*1000));
		int month = getMonth(dateStr);
		//�����ǰ��month�Ѿ���������ˮ��¼��,��ֱ�ӷ�����ˮ�еĽ��
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
		//----����ǰ����Ϊ����������
		if ((gapArray[0]!=1) || (gapArray[1]!=1)) {
			return 0.0;
		}
		//----����������2�����򲻽���Ԥ��
		for (int i = 2; i < cnt; i++) {
			if (gapArray[i] > 2) {
				return 0.0;
			}
		}
		//(2)������Ч�Լ�����,��������Ч��
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
		if (commentString.contains("��Ʊ")) {
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
				System.out.print("����"+mn+"������:");
				for (InstallmentData elem : fenqiSet) {
					int nn = elem.getPeriods()-elem.getCurrent();
					System.out.print(elem.getDate()+"��(��ʣ"+nn+"��);");
				}
				System.out.println("");
			} else {
				System.out.println("��������:" + var.getDate() + "��");
			}
			for (int j = 0; j < m; j++) {
				System.out.println(flowDataList.get(j).getDateStr()+"---"+flowDataList.get(j).getCommentFullStr()+"---$"+flowDataList.get(j).getPrice());
			}
			System.out.println("--------------------------------");
		}		
	}
		
	//��������Ҫ������
	public HashMap<Long, ArrayList<FlowData>> getRegularityDataMap(){
		return flowdataMap;
	}
	//�ж�����������Ƿ���뵱ǰʱ��̫Զ
	private boolean isDateNearLengthValid(long currentTimeStamp, FlowData flowData, int len) {
		//�����������������ֱ�ӷ���
		if (len <= 5) {
			return false;
		}
		//����currentTimeStamp��������
		long timeStamp = flowData.getUnixStamp();
		int nDays = getNDays(timeStamp, currentTimeStamp);
		if ((nDays>APART_DAYS) && (timeStamp<currentTimeStamp)) {
			return false;
		}			
		
		return true;
	}
		
	//�ж���Ϊ������������
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
		//ͳ�Ʒ��ϼ����������
		//1. ���һ����
		for (int i = 0; i < m; i++) {
			int days = jianGeRiQiList.get(i);			
			if ((days>=20) && (days<=35)) {
				cnt1++;
			}
		}		
		ratio1 = 1.0*cnt1/m;
		//2. ���������
		for (int i = 0; i < m; i++) {
			int days = jianGeRiQiList.get(i);
			if ((days>32) && (days<=65)) {
				cnt2++;
			}
		}		
		ratio2 = 1.0*cnt2/m;
		//3. ���������
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
	
	//Ѱ��ʱ���������¼�
	//freqTh: ʱ�������ֵ�Ƶ����ֵ
	private int findEqualIntervalTime(ArrayList<FlowData> flowDataList, int freqTh, double threshold){
		int n = flowDataList.size();
		ArrayList<Integer> jianGeRiQiList = new ArrayList<Integer>();
		for (int i = n-1; i >= 1; i--) {
			int days = getNDays(flowDataList.get(i).getUnixStamp(), flowDataList.get(i-1).getUnixStamp());
			jianGeRiQiList.add(Integer.valueOf(days));			
		}
		int m = jianGeRiQiList.size();
		//ͳ�Ʒ��ϼ����������
		int avgDays = 0;
		int maxDay = 0; //��¼����ʱ��������
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
		//�ж��Ƿ��кܴ��ʱ����,����˵����3��������
		if ((maxDay>90) && (daysGap.get(maxDay).intValue()==1)) {
			return -1;
		}
		//Ѱ�ҳ���Ƶ����ߵļ������
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
		//��ſ����Ƿ��Ϲ��ɵļ������
		ArrayList<Integer> nDaysList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer == elem.getValue().intValue()) {
				nDaysList.add(elem.getKey());
			}
		}
		//ֻ��һ�����ֵ
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
		//�ж�����ֵ
		ArrayList<Integer> jianGeArrayList = new ArrayList<Integer>(); //��¼���ʵ�ʱ����
		ArrayList<Integer> ndaysList2 = new ArrayList<Integer>(); //��¼���ֵĴ���
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
		//����"��-��"map
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
		//����ƽ�����ڳ��ֵ�Ƶ��
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
		//ͳ�Ʒ��ϼ����������
		int avgDays = 0;
		int maxDay = 0; //��¼����ʱ��������
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
		//�ж��Ƿ��кܴ��ʱ����,����˵����3��������
		if ((maxDay>90) && (daysGap.get(maxDay).intValue()==1)) {
			return -1;
		}
		//Ѱ�ҳ���Ƶ����ߵļ������
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
		//��ſ����Ƿ��Ϲ��ɵļ������
		ArrayList<Integer> nDaysList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : daysGap.entrySet()) {
			if (maxer == elem.getValue().intValue()) {
				nDaysList.add(elem.getKey());
			}
		}
		//ֻ��һ�����ֵ
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
		//�ж�����ֵ
		ArrayList<Integer> jianGeArrayList = new ArrayList<Integer>(); //��¼���ʵ�ʱ����
		ArrayList<Integer> ndaysList2 = new ArrayList<Integer>(); //��¼���ֵĴ���
		n = nDaysList.size();
		for (int i = 0; i < n; i++) {
			days = nDaysList.get(i); //�������
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
		//ͳ�Ƴ������ֵ��ʱ����
		int sum1 = 0, sum2 = 0;
		for (int i = 0; i < n; i++) {
			if (ndaysList2.get(i).intValue() == maxer) {
				Integer tmp = jianGeArrayList.get(i);
				sum1 += daysGap.get(tmp).intValue(); //����������ۼ�
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
	
	//***���˶�γ��ֵ���ͬ���ڷ�***
	private ArrayList<DateTimeStamp> deRepetitionDate(ArrayList<FlowData> flowDataList){
		//ȥ��: ��ͬ���ڵ���ֻ��һ��
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
	//***ʱ�����ĺ�����***
	private int isEqualTimeInterval(ArrayList<DateTimeStamp> dateTimeStampList, double topTh, double ratio) {
		//����ʱ����gap,��˳��:Զ->��
		ArrayList<Integer> timeIntervalList = new ArrayList<Integer>();
		int n = dateTimeStampList.size();
		for (int i = 1; i < n; i++) {
			int days = getNDays(dateTimeStampList.get(i-1).getStamp(), dateTimeStampList.get(i).getStamp());
			timeIntervalList.add(days);
		}
		//ͳ��ʱ����gap�ĳ��ִ���
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
		//�жϼ�����ֵĺ�����
		int maxCnt = 0; //���ִ�������ʱ����
		int gaps = 0;
		for (Map.Entry<Integer, Integer> elem : gapFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
				gaps = elem.getKey().intValue();
			}
		}
		//������ļ�����ִ���==1���߼����Ϊ1,����Ϊ����̫Ƶ��,�����˳�
		if ((maxCnt==1) || (gaps==0) || (gaps==1)) {
			return -1;
		}
		//������ִ�������ʱ����ռ�еı�������topTh,����Ϊ��
		//��ѡ����gap������չ���ж��Ƿ�Ϊ����ʱ����
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
		//���ռ�б�������,�����ɽ���Զ��ʱ��˳��,�ж�ʱ�����ĺ�����
		//ѡȡ�������50%
		btm = (int)(n*ratio);
		//���ȡ��������������������,��
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
	
	//***�������ڵ�ѡ��***
	private int isRegularDate(ArrayList<DateTimeStamp> dateTimeStampList, int nEffectiveDays) {
		//ͳ��"��"�ĳ���Ƶ��
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
		//Ѱ���������ڳ��ִ���
		int maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (maxCnt < tmp) {
				maxCnt = tmp;
			}
		}
		//������ִ������Ϊ1
		if (maxCnt == 1) {
			return 0;
		}
		//�����ִ���==maxCnt��date������
		ArrayList<Integer> maxFreqList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				maxFreqList.add(elem.getKey());
			}
		}
		//�Ӻ�ѡ�������ҵ��������
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
		//������Ƶ��һ�������ڱ�������
		ArrayList<Integer> resDateList = new ArrayList<Integer>();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				resDateList.add(elem.getKey());
			}
		}
		//ѡ����ڳ���Ƶ�ʽϸߵ�����
		//ѡ���ͳ�����ݳ���ΪnEffectiveDays
		m = dateTimeStampList.size();
		if (m < nEffectiveDays) {
			return dateTimeStampList.get(m-1).getDateVal();
		}
		n = m - nEffectiveDays;
		dateFreqMap.clear();
		//ͳ�ƺ�nEffectiveDays��ʱ���ڳ��ֵ����ڵ�Ƶ��dateFreqMap
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
		//����Ƶ���������ڵĺ�ѡ�б�maxFreqList
		maxFreqList.clear();
		for (Map.Entry<Integer, Integer> elem : dateFreqMap.entrySet()) {
			if (elem.getValue().intValue() == maxCnt) {
				maxFreqList.add(elem.getKey());
			}
		}
		//���ֻ����һ������,���жϸ������Ƿ������ͳ���б�resDateList��,���������
		if (maxFreqList.size() == 1) {
			Integer dateTmp = maxFreqList.get(0);
			for (Integer integer : resDateList) {
				if (integer.equals(dateTmp)) {
					return dateTmp.intValue();
				}
			}
			return dateTimeStampList.get(m-1).getDateVal();
		} 
		//���ֶ������
		return dateTimeStampList.get(m-1).getDateVal();		
	}
	
	
	//***�������ڹ������ж�***
	private HashSet<Integer> isRegularWeek(ArrayList<DateTimeStamp> dateTimeStampList){
		int n = dateTimeStampList.size();
		HashMap<Integer, Integer> weekMap = new HashMap<Integer, Integer>();
		HashSet<Integer> weekSet = new HashSet<Integer>();
		//ͳ�����ڳ��ֵĴ���
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
		//���ÿ��var�ĳ��ִ�����ƽ��������಻���ʱ��,����Ϊ���й�������
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
		//���"����"û�з���ǰһ������,����н��������ж�
		int beg = (int)(0.5*n);
		int end = n;
		if ((end-beg+1) < 6) {
			return null;
		}
		for (int i = 0; i < end; i++) {
			
		}
		return weekSet;
	}
	
	//��ȡ���������ݼ�¼
	public HashMap<Integer, ArrayList<FlowData>> getClusterDataListMap(ArrayList<FlowData> dataList, ArrayList<Integer> descFreqMapSortList, HashMap<Integer, HashSet<Integer>> relevanceIdSetMap) {
		HashSet<Integer> idFlgSet = new HashSet<Integer>(); //��¼�ù���id
		HashMap<Integer, ArrayList<FlowData>> clusterFlowDataMap = new HashMap<Integer, ArrayList<FlowData>>();
		int n = descFreqMapSortList.size();
		for (int i = 0; i < n; i++) {
			Integer idx = descFreqMapSortList.get(i);
			//�����id�Ѿ�����,���ų�
			if (idFlgSet.contains(idx)) {
				continue;
			}
			//����б���С��3���ų�
			HashSet<Integer> idxSet = relevanceIdSetMap.get(idx);
			if (idxSet.size() <= 3) {
				idFlgSet.add(idx);
				continue;
			}
			//����index�б�,�����µ��б�
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
	
	//�۸��ж�
	//���ط��������ı���
	//threshold : �����������С����ֵ,�򲻷��Ϲ���
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
	
	//����ʱ�����������
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

	//�ж��Ƿ�Ϊ���ڸ�������
	private boolean isInstallmentData(ArrayList<FlowData> flowDataList) {
		//regex
		Pattern pattern1 = Pattern.compile("[0-9]{2}-[0-9]{2}");
		Pattern pattern2 = Pattern.compile("[0-9]{2}/[0-9]{2}");		
		//�ж��Ƿ�Ϊ"����-�绰����"��ʽ
		Pattern pattern3 = Pattern.compile("[0-9]{3}-[0-9]{4}");
		//�ж��Ƿ�Ϊ������ʽ
		Pattern pattern4 = Pattern.compile("[0-9]{2}-[0-9]{2}-[0-9]{2}");
		//
		Pattern pattern5 = Pattern.compile("[0-9]{2}/[0-9]{3}");
		//"��1�ڹ�3��"
		Pattern pattern6 = Pattern.compile("��[0-9]�ڹ�[0-9]��");
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
		//�Ƿ��ڸ���
		if (flg == 1) {
			return false;
		}
		return true;
	}
	//�������ڸ�������
	private HashSet<InstallmentData> installmentDataProcess(ArrayList<FlowData> flowDataList){
		Pattern pattern1 = Pattern.compile("[0-9]{2}-[0-9]{2}");
		Pattern pattern2 = Pattern.compile("[0-9]{2}/[0-9]{2}");
		Pattern pattern3 = Pattern.compile("��[0-9]�ڹ�[0-9]��");
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
	
	//��ȡ���Ƶõ�������
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
		//Ѱ�ҳ��ִ�����������
		int maxCnt = 0;
		for (Map.Entry<Integer, Integer> elem : dateCntMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (tmp > maxCnt) {
				maxCnt = tmp;
			}
		}
		//�������й������ڵ��б�
		ArrayList<Integer> dateList = new ArrayList<Integer>(); 
		for (Map.Entry<Integer, Integer> elem : dateCntMap.entrySet()) {
			int tmp = elem.getValue().intValue();
			if (tmp == maxCnt) {
				dateList.add(elem.getKey());
			}
		}		
		int gap = 2;
		//���ֻ��һ��Ψһ���ھ���������
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
		//���ڶ�����ڽ����ٴ��ж�
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
	
	//�ж�: ���һ�����ڳ��ֶ�θ����¼����Ϊ�Ƿǹ�������
	//1.����˵��γ�����ͬ��ʱ��: 01/05/2014
	//2.����:01/05/2014, 02/05/2014, 03/05/2014
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
	//�ж��Ƿ�
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
	//��������ʱ���,������������
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
	//���ء�y/m/d���е�d
	private int getDate(String dateStr){
		//String string = "24/03/2014 07:05:42";
		String[] strs = dateStr.split("/");
		//System.out.println("asfdad = "+strs.length+":"+strs[0]);
		if (strs.length <= 1) {
			return 0;
		}
		return Integer.valueOf(strs[0]).intValue();
	}
	//����"��"
	private int getMonth(String dateStr){
		String[] strs = dateStr.split("/");
		if (strs.length <= 1) {
			return 0;
		}
		return Integer.valueOf(strs[1]).intValue();
		
	}
	//����"��-��-��"����
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
	//����"��-��-��"����
	private String getYearMonthRiStr(String timeString) {
		String[] strs = timeString.split(" ");		
		if (strs.length <= 1) {
			return null;
		}   
		return strs[0];
	}
	//����"��-��"�ַ���
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
	//����"��-��"�ַ���
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
