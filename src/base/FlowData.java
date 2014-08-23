package base;

public class FlowData {
	private long id; //自增id
	private long uid; //用户id
	private long unixStamp; //时间戳
	private String payType; //付款类型
	private long price; //交易金额
	private String commentStr; //滤过后的字符串
	private String commentFullStr; //原字符串
	private String bankNameStr; //银行名字
	private String consumeTypeStr; //消费类型(消费分类)
	private int cardid; //()
	private String dateStr; //日-月-年  时间  字符串
	private String YearMonthDateStr; //日-月-年   字符串
	private int dateRule; //估算的日期(?日)
	private String weekString;
	private int weekInt;	
	
	public FlowData() {
		super();
	}
	public FlowData(FlowData flowdata) {
		super();
		this.id = flowdata.id;
		this.uid = flowdata.uid;
		this.unixStamp = flowdata.unixStamp;
		this.payType = flowdata.payType;
		this.price = flowdata.price;
		this.commentStr = flowdata.commentStr;
		this.commentFullStr = flowdata.commentFullStr;
		this.bankNameStr = flowdata.bankNameStr;
		this.consumeTypeStr = flowdata.consumeTypeStr;
		this.cardid = flowdata.cardid;
		this.dateStr = flowdata.dateStr;
		this.dateRule = flowdata.dateRule;
		this.weekString = flowdata.weekString;
		this.weekInt = flowdata.weekInt;
		this.YearMonthDateStr = flowdata.YearMonthDateStr;
	}
	//
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public long getUnixStamp() {
		return unixStamp;
	}
	public void setUnixStamp(long unixStamp) {
		this.unixStamp = unixStamp;
	}
	public String getPayType() {
		return payType;
	}
	public void setPayType(String payType) {
		this.payType = payType;
	}
	public long getPrice() {
		return price;
	}
	public void setPrice(long price) {
		this.price = price;
	}
	public String getCommentStr() {
		return commentStr;
	}
	public void setCommentStr(String commentStr) {
		this.commentStr = commentStr;
	}
	public String getCommentFullStr() {
		return commentFullStr;
	}
	public void setCommentFullStr(String commentFullStr) {
		this.commentFullStr = commentFullStr;
	}
	public String getBankNameStr() {
		return bankNameStr;
	}
	public void setBankNameStr(String bankNameStr) {
		this.bankNameStr = bankNameStr;
	}
	public String getConsumeTypeStr() {
		return consumeTypeStr;
	}
	public void setConsumeTypeStr(String consumeTypeStr) {
		this.consumeTypeStr = consumeTypeStr;
	}
	public int getCardid() {
		return cardid;
	}
	public void setCardid(int cardid) {
		this.cardid = cardid;
	}
	public String getDateStr() {
		return dateStr;
	}
	public void setDateStr(String dateStr) {
		this.dateStr = dateStr;
	}
	public int getDateRule() {
		return dateRule;
	}
	public void setDateRule(int dateRule) {
		this.dateRule = dateRule;
	}
	public String getWeekString() {
		return weekString;
	}
	public void setWeekString(String weekString) {
		this.weekString = weekString;
	}
	public int getWeekInt() {
		return weekInt;
	}
	public void setWeekInt(int weekInt) {
		this.weekInt = weekInt;
	}
	public String getYearMonthDateStr() {
		return YearMonthDateStr;
	}
	public void setYearMonthDateStr(String yearMonthDateStr) {
		YearMonthDateStr = yearMonthDateStr;
	}	
	
}
