package com.timeinc.mageng.arkdistributor;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Connie Shi
 * Application class for existing applications/magazines in iTunesConnect
 */
public class Application {
	private String vendorId, appleId, applicationId, type, clearedForSale, screenshotPath, screenshotName,
		screenshotSize, screenshotChecksum;
	static final Logger logger = LogManager.getLogger(Application.class);
	
	public Application(String vendorId, 
			String appleId, 
			String applicationId, 
			String type, 
			String clearedForSale, 
			String screenshotPath ) {
		this.vendorId = 			vendorId;
		this.appleId = 				appleId;
		this.applicationId = 		applicationId;
		this.type = 				type;
		this.clearedForSale = 		clearedForSale;
		this.screenshotPath = 		screenshotPath;
		screenshotName = 			new File(screenshotPath).getName();
		screenshotSize = 			new File(screenshotPath).length()+"";
		screenshotChecksum = 		getMD5Checksum(screenshotPath);
	}
	
	/**
	 * Calculate the md5 checksum value of screenshot
	 * @return
	 */
	public String getMD5Checksum(String screenshot){
		StringBuffer sb = new StringBuffer();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			FileInputStream fis = new FileInputStream(screenshot);

			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			byte[] mdbytes = md.digest();
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			fis.close();
		}
		catch (Exception e) {
			logger.catching(e);
		}
		return sb.toString();
	}

	/**
	 * Vendor ID for company
	 * @return
	 */
	public String getVendorId() {
		return vendorId;
	}

	/**
	 * Corresponding ID in iTunesConnect
	 * @return
	 */
	public String getAppleId() {
		return appleId;
	}

	/**
	 * Corresponding applicationID in MySQL Ark Database
	 * @return
	 */
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * Types include "free-subscription", etc.
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * clearedForSale = true/false
	 * @return
	 */
	public String getClearedForSale() {
		return clearedForSale;
	}

	/**
	 * Path of screenshot
	 * @return
	 */
	public String getScreenshotPath() {
		return screenshotPath;
	}

	/**
	 * Name of screenshot
	 * @return
	 */
	public String getScreenshotName() {
		return screenshotName;
	}

	/**
	 * Screen shot size
	 * @return
	 */
	public String getScreenshotSize() {
		return screenshotSize;
	}

	/**
	 * Check sum required for upload of metadata.xml
	 * @return
	 */
	public String getScreenshotChecksum() {
		return screenshotChecksum;
	}

	/**
	 * Log4J for debugging
	 * @return
	 */
	public static Logger getLogger() {
		return logger;
	}
}
