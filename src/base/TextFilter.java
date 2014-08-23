package base;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TextFilter {
	private HashSet<String> fullStopWordSet = new HashSet<String>();
	private HashSet<String> containStopWordSet = new HashSet<String>();
	private ArrayList<String> keywordList = new ArrayList<String>();
	private HashMap<String, String> mappingMap = new HashMap<String, String>();
	private int fullStopWordSetSize = 0;
	private int containStopWordSetSize = 0;
	private int keywordListSize = 0;
	
	public TextFilter(String dirName) {
		try {
			loadStopWordsFile(dirName+"/stopwords.txt");
			loadKeyords(dirName+"/keywords_filter.txt");
			loadKeywordNameMap(dirName+"/keywords_match.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		fullStopWordSetSize = fullStopWordSet.size();
		containStopWordSetSize = containStopWordSet.size();
		keywordListSize = keywordList.size();
		//System.out.println("full = " + fullStopWordSetSize + "; contain = " + containStopWordSetSize);
	}
	//载入停止词文件
	private void loadStopWordsFile(String path) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(path));
		BufferedReader br = new BufferedReader(isr);
		String lineStr = null;
		//读取全词匹配关键词
		while ((lineStr = br.readLine()) != null) {
			if (lineStr.equals("xxxxxx")) {
				break;
			}
			fullStopWordSet.add(lineStr);
			//System.out.println(lineStr);
		}
		//读取包含匹配关键词
		while ((lineStr = br.readLine()) != null) {
			containStopWordSet.add(lineStr);
			//System.out.println(lineStr);
		}
		br.close();
		isr.close();
	}	
	
	//判断是否包含停止词
	//public boolean containStopWord(String inputStr) {
	public boolean isValidDescriptionString(String inputStr) {
		if (null == inputStr) {
			return false;
		}
		else if (inputStr.length()<=3) {
			return false;
		} 	
		for (String elem : fullStopWordSet) {
			if (inputStr.contains(elem)) {
				return false;
			}
		}
		String str = getFilteredString(inputStr);
		if (isRepeatName(str)) {
			return false;
		}
		for (String elem : containStopWordSet) {
			if (str.contains(elem)) {
				return false;
			}
		}
		
		return true;
	}
		
	//返回过滤完后的字符串
	public String getFilteredString(String inputStr) {
		String str = inputStr.replaceAll("[\\p{Punct}\\p{Space}]+", "");
		str = str.replaceAll("[0-9A-Za-zＡ-Ｙａ－ｚＡ－Ｚ]*", "");
		str = str.replaceAll("[！，。：《》【】（）★￥*&－０１２３４５６７８９]", "");
		//判断是否为纯字母 或者数字
		if ((str.length()==0)) {
			boolean flg = inputStr.matches("[a-zA-Z0-9]*");
			if (flg) {
				return inputStr;
			}
		}
		return str;
	}
	//判读字符串是否是重复短语
	private boolean isRepeatName(String commentStr){
		int len = commentStr.length();
		if (0 == len) {
			return false;
		}
		//2,3
		if ((len==4) || (len==6) || (len==10)) {
			int pos = len/2;
			String str1 = commentStr.substring(0, pos);
			String str2 = commentStr.substring(pos, len);
			if (str1.equals(str2)) {
				return true;
			}
		}
		
		return false;
	}	
	//载入"筛选词-规律名称"映射文件
	public void loadKeywordNameMap(String path) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(path));
		BufferedReader br = new BufferedReader(isr);
		String lineStr = null;
		while ((lineStr = br.readLine()) != null) {
			String[] words = lineStr.split("=");
			if (words.length != 2) {
				continue;
			}
			mappingMap.put(words[0], words[1]);
		}		
		br.close();
		isr.close();		
	}
	//返回映射表map
	public HashMap<String, String> getMappingMap() {
		return mappingMap;
	}
	//载入筛选关键词
	public void loadKeyords(String path) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(path));
		BufferedReader br = new BufferedReader(isr);
		String lineStr = null;
		while ((lineStr = br.readLine()) != null) {			
			if (lineStr.length() == 0) {
				continue;
			}
			keywordList.add(lineStr);
		}		
		br.close();
		isr.close();
	}
	//判断是否包含了筛选关键词
	public boolean isContainKeyword(String commentStr) {
		for (int i = 0; i < keywordListSize; i++) {
			if (commentStr.contains(keywordList.get(i))) {
				return true;
			}
		}
		return false;
	}	

	public static void main(String[] args) {
		TextFilter var = new TextFilter("E:/Code/java/CalcRegularityData/config");
		HashMap<String, String> mappingHashMap = var.getMappingMap();
		System.out.println("size = " + mappingHashMap.size());
//		String testString = "ＥＵＹＡＮＳＡＮＧ－ＴＳＴＥＵＹＡＮＳＡＮＧ－ＴＳＴH&M阅读《  特斯拉来电《快公司》月刊》交易号１３年２０期";
//		
//		System.out.println(var.getFilteredString("中国afaAAAA021545"));
//		if (!var.isValidDescriptionString(testString)) {
//			System.out.println("Don't contain!");
//		}
		

	}

}
