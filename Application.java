package com.timeinc.mageng.arkdistributor;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Connie Shi
 * Application class for existing applications in iTunesConnect
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
		try{
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

	public String getVendorId() {
		return vendorId;
	}

	public String getAppleId() {
		return appleId;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public String getType() {
		return type;
	}

	public String getClearedForSale() {
		return clearedForSale;
	}

	public String getScreenshotPath() {
		return screenshotPath;
	}

	public String getScreenshotName() {
		return screenshotName;
	}

	public String getScreenshotSize() {
		return screenshotSize;
	}

	public String getScreenshotChecksum() {
		return screenshotChecksum;
	}

	public static Logger getLogger() {
		return logger;
	}
}
