package base;

public class DateTimeStamp {
	private String yearMonthDateStr = null;
	private long stamp = 0l;
	private int week = 0;//1-7
	private int dateVal = 0; //"日"对应的数值
	
	public DateTimeStamp(String yearMonthDateStr, long stamp,  int week, int dateVal) {
		super();
		this.yearMonthDateStr = yearMonthDateStr;
		this.stamp = stamp;
		this.week = week;
		this.dateVal = dateVal;
	}
	public DateTimeStamp() {
		super();	
	}
	public String getYearMonthDateStr() {
		return yearMonthDateStr;
	}
	public void setYearMonthDateStr(String yearMonthDateStr) {
		this.yearMonthDateStr = yearMonthDateStr;
	}
	public long getStamp() {
		return stamp;
	}
	public void setStamp(long stamp) {
		this.stamp = stamp;
	}	
	public int getWeek() {
		return week;
	}
	public void setWeek(int week) {
		this.week = week;
	}	
	public int getDateVal() {
		return dateVal;
	}
	public void setDateVal(int dateVal) {
		this.dateVal = dateVal;
	}
	//
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (stamp ^ (stamp >>> 32));
		result = prime
				* result
				+ ((yearMonthDateStr == null) ? 0 : yearMonthDateStr.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DateTimeStamp other = (DateTimeStamp) obj;
		if (stamp != other.stamp)
			return false;
		if (yearMonthDateStr == null) {
			if (other.yearMonthDateStr != null)
				return false;
		} else if (!yearMonthDateStr.equals(other.yearMonthDateStr))
			return false;
		return true;
	}	
}
