package base;

public class FlowData {
	private long id; //����id
	private long uid; //�û�id
	private long unixStamp; //ʱ���
	private String payType; //��������
	private long price; //���׽��
	private String commentStr; //�˹�����ַ���
	private String commentFullStr; //ԭ�ַ���
	private String bankNameStr; //��������
	private String consumeTypeStr; //��������(���ѷ���)
	private int cardid; //()
	private String dateStr; //��-��-��  ʱ��  �ַ���
	private String YearMonthDateStr; //��-��-��   �ַ���
	private int dateRule; //���������(?��)
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
