package base;

public class InstallmentData {	
	private int periods;
	private long price;
	private int priceInt;
	private int current;
	private int date;

	public InstallmentData(int periods, long price, int priceInt, int current,
			int date) {
		super();
		this.periods = periods;
		this.price = price;
		this.priceInt = priceInt;
		this.current = current;
		this.date = date;
	}
	public InstallmentData(){
		super();
	}
	//
	public int getPeriods() {
		return periods;
	}
	public void setPeriods(int periods) {
		this.periods = periods;
	}
	public long getPrice() {
		return price;
	}
	public void setPrice(long price) {
		this.price = price;
	}
	public int getPriceInt() {
		return priceInt;
	}
	public void setPriceInt(int priceInt) {
		this.priceInt = priceInt;
	}
	public int getCurrent() {
		return current;
	}
	public void setCurrent(int current) {
		this.current = current;
	}
	public int getDate() {
		return date;
	}
	public void setDate(int date) {
		this.date = date;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + date;
		result = prime * result + periods;
		result = prime * result + priceInt;
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
		InstallmentData other = (InstallmentData) obj;
		if (date != other.date)
			return false;
		if (periods != other.periods)
			return false;
		if (priceInt != other.priceInt)
			return false;
		return true;
	}	
}