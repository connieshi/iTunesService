package com.timeinc.mageng.arkdistributor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Scanner; 
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerStream;

/**
 * @author Connie Shi
 * Uses Transporter to obtain and generate metadata.xml to upload
 */
public class ITunesService {

	private static Logger logger = LogManager.getLogger(ITunesService.class.getName());
	private ArrayList<Application> applications;
	private ArkDAO ark;
	
	/**
	 * @param configFile
	 */
	public ITunesService(File configFile) {
		//Get ArrayList of all Applications in config.xml
		getApplicationsList(configFile);
		
		//Update iTunesConnect with InAppPurchases for every Application
		updateiTunesForAll(applications);
	}
	
	/**
	 * Copies screenshot file from path to itmsp package
	 * @param src
	 * @param dst
	 */
	public void copyFile(File src, File dst) {
	    long p = 0, dp = 0, size = 0;
	    FileChannel in = null, out = null;

	    try {
	        if (!dst.exists()) 
	        	dst.createNewFile();

	        in = new FileInputStream(src).getChannel();
	        out = new FileOutputStream(dst).getChannel();
	        size = in.size();

	        while ((dp = out.transferFrom(in, p, size)) > 0) {
	            p += dp;
	        }
	    }
	    catch (Exception e) {
	    	logger.catching(e);
	    }
	}
	
	/**
	 * Get ArrayList of all Applications in config.xml
	 * @param configuration
	 * @return
	 */
	private void getApplicationsList(File configuration) {
		try {
			applications = ITMSConfiguration.readConfigFile(configuration);
		} 
		catch (Exception e) {
			logger.catching(e);
		}
		if (applications.size() == 0) {
			logger.info("No applications listed in config file.");
			System.exit(0);
		}
	}
	
	/**
	 * Loop through list to update iTunesConnect for every Application
	 * @param list
	 */
	private void updateiTunesForAll(ArrayList<Application> list) {
		for (int i = 0; i < list.size(); i++) {
			updateiTunesForEach(list.get(i));
		}
	}

	/**
	 * For each Application in ArrayList, read itms file, check duplicates with Ark, and generate XML
	 * @param applications
	 */
	private void updateiTunesForEach(Application application) {
		try {
			//Get itms package from Transporter, get list of InAppPurchases from original metadata.xml file
			//Compare with Ark records and remove duplicates
			//Generate new metadata.xml and upload if there is an update
			retrieveFile(application);
			ArrayList<InAppPurchase> inapps = ITMSConfiguration.readITMSFile(application);
			checkArkWithITMS(application, inapps);

			if (ark.getListFromArk().size() == 0) {
				logger.info("No new entries in Ark from last update for " + application.getVendorId());
			} 
			else {
				logger.info(ark.getListFromArk().size() + " In App Purchase(s) to upload.");
				
				for (InAppPurchase p : ark.getListFromArk()) {
					logger.info("Application: " + p.toString());
				}
				
				ITMSConfiguration.generateXML(ark.getListFromArk(), application);
				copyFile(new File(application.getScreenshotPath()), 
						new File(ITMSConfiguration.getTempFolder()+"/"+
						application.getVendorId()+".itmsp/"+application.getScreenshotName()));
						
				uploadPackage(application);
			}
		}
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Check listFromArk for duplicate InAppPurchases, delete if exist
	 * @param apps
	 * @param inAppList
	 */
	private void checkArkWithITMS(Application apps, ArrayList<InAppPurchase> inAppList) {
		try {
			ark = new ArkDAO(ITMSConfiguration.getArkUser(), 
					ITMSConfiguration.getArkPassword(), apps); 
			
			logger.info(ark.getListFromArk().size() + " In App Purchase(s) obtained from Ark.");
			
			//If productId/referenceId already exists in iTunesConnect, do nothing
			for (InAppPurchase inappPurchase : inAppList) {
				if (ark.getListFromArk().contains(inappPurchase)) {
					ark.getListFromArk().remove(inappPurchase);
				}
			}
			
			logger.info(ark.getListFromArk().size() + " In App Purchase(s) to upload"
					+ " after removing existing In App Purchases.");
		}
		catch(Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Get itmps package from iTunesConnect
	 * @param app
	 */
	private void retrieveFile(Application app) {
		try {
			Process process = Runtime.getRuntime().exec("sh " + ITMSConfiguration.getPathToTransporter()
					+ " -m lookupMetadata"
					+ " -u " + 				ITMSConfiguration.getiTunesUser()
					+ " -p " + 				ITMSConfiguration.getiTunesPassword()
					+ " -vendor_id " + 		app.getVendorId()
					+ " -destination " + 	ITMSConfiguration.getTempFolder() );
			getTransporterStream(process);
			process.waitFor();
		}
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Upload Application package to iTunesConnect
	 * @param app
	 */
	private void uploadPackage(Application app) {
		try {
			Process process = Runtime.getRuntime().exec("sh " + ITMSConfiguration.getPathToTransporter()
					+ " -m upload"
					+ " -f " + ITMSConfiguration.getTempFolder()+"/"+app.getVendorId()+".itmsp"
					+ " -u " + ITMSConfiguration.getiTunesUser()
					+ " -p " + ITMSConfiguration.getiTunesPassword() );
			getTransporterStream(process);
			process.waitFor();
		}
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Obtain output stream from Transporter for Logger
	 * @param p
	 */
	private static void getTransporterStream(Process p) {
		try {
			LoggerStream ls = logger.getStream(Level.DEBUG);

			inheritIODebug(p.getInputStream(), ls);
			inheritIODebug(p.getErrorStream(), ls);
		}
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Debugging for logger
	 * @param src
	 * @param dest
	 */
	private static void inheritIODebug(final InputStream src, final PrintStream dest) {
		new Thread (new Runnable() {
			public void run() {
				Scanner sc = new Scanner(src);
				while (sc.hasNextLine()) {
					logger.debug(sc.nextLine());
				}
				sc.close();
			}
		}).start();
	}
	
	public static void main(String[]args) {
		if (args.length != 1) {
			logger.info("Usage: java -jar ITunesService.jar -configfile path");
			System.exit(0);
		}
		File configuration = new File(args[0]);
		if (!configuration.isFile()) {
			logger.info("Invalid file path");
			System.exit(0);
		}
		if ( !configuration.canRead() ) {
			logger.info("File cannot be read");
			System.exit(0);
		}
		new ITunesService(configuration);
	}
}

