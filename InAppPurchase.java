package com.timeinc.mageng.arkdistributor;

/**
 * @author shic
 * InAppPurchase to upload to iTunesConnect
 */
public class InAppPurchase{
	private String productId, 
		referenceName, 
		wholesalePriceTier,
		type;
	private double price;
	
	public InAppPurchase(
			String productId, 
			String referenceName, 
			double price) {
		this.productId = 			productId;
		this.referenceName = 		referenceName;
		this.price = 				price;
		wholesalePriceTier = 		getPriceTier(price);
		this.type = 				findType(wholesalePriceTier);
	}
	
	public String getProductId() {
		return productId;
	}

	public String getReferenceName() {
		return referenceName;
	}

	public String getWholesalePriceTier() {
		return wholesalePriceTier;
	}

	@Override
	public String toString() {
		return "InAppPurchase [referenceName=" + referenceName + "]";
	}

	public double getPrice() {
		return price;
	}
	
	/**
	 * Generate locale description
	 * @param name
	 * @return
	 */
	public String getLocaleDescription(String name) {
		return "This is the " + name + " issue.";
	}

	/**
	 * Get price tier by rounding up, unless it is 0 (free)
	 * @param price
	 * @return
	 */
	private String getPriceTier(double priceInDouble) {
		if (priceInDouble > 0.00) {
			int tier = (int)priceInDouble + 1;
			return tier+"";
		}
		else return "0";
	}
	
	/**
	 * Find the type of the In App Purchase
	 * @param wholesalePriceTier
	 * @return
	 */
	private String findType(String wholesalePriceTier) {
		if (wholesalePriceTier.equals("0"))
			return "free-subscription";
		else return "non-consumable";
	}
	
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((productId == null) ? 0 : productId.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof InAppPurchase))
			return false;
		InAppPurchase other = (InAppPurchase) obj;
		if (productId == null) {
			if (other.productId != null)
				return false;
		} else if (!productId.equals(other.productId))
			return false;
		return true;
	}
}
