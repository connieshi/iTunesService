package com.timeinc.mageng.arkdistributor;
import java.io.File;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Connie Shi
 * Read from configuration.xml and write to metadata.xml to upload to iTunesConnect
 */
public class ITMSConfiguration {
	
	static final Logger logger = LogManager.getLogger(ITMSConfiguration.class);
	private static ArrayList<InAppPurchase> appsFromITMS = new ArrayList<InAppPurchase>();
	private static String arkUser, arkPassword, arkUrl, tempFolder, iTunesUser, iTunesPassword, teamId, pathToTransporter;
	private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder dBuilder;
	private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private static Transformer transformer;
	private static ArrayList<Application> applications = new ArrayList<Application>();

	/**
	 * Prevents instantiation 
	 */
	private ITMSConfiguration() {}

	/**
	 * Read config file, store Applications in ArrayList
	 * @param pathToConfig
	 * @return
	 */
	public static ArrayList<Application> readConfigFile(File pathToConfig) {
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(pathToConfig);
			doc.getDocumentElement().normalize();

			//Same user info for every application
			readUserInfo(doc);

			//Data specific to individual applications used to create Applications object and add to ArrayList
			readApplicationInfo(doc, applications);
		} catch (Exception e) {
			logger.catching(e);
		}
		return applications;
	}
	
	/**
	 * Read user information from config file
	 * @param doc
	 */
	private static void readUserInfo(Document doc) {
		NodeList n = doc.getElementsByTagName("accounts");
		for (int temp = 0; temp < n.getLength(); temp++) {
			Node nNode = n.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				iTunesUser = eElement.getElementsByTagName("itunes-account").item(0).getTextContent();
				iTunesPassword = eElement.getElementsByTagName("itunes-password").item(0).getTextContent();
				teamId = eElement.getElementsByTagName("itunes-teamid").item(0).getTextContent();
				arkUrl = eElement.getElementsByTagName("ark-db-url").item(0).getTextContent();
				arkUser = eElement.getElementsByTagName("ark-db-username").item(0).getTextContent();
				arkPassword = eElement.getElementsByTagName("ark-db-password").item(0).getTextContent();
				pathToTransporter = eElement.getElementsByTagName("path-to-transporter").item(0).getTextContent();
				tempFolder = eElement.getElementsByTagName("temp-folder").item(0).getTextContent();
			}		
		}
	}
	
	/**
	 * Read each application info from config file and put Application object in list
	 * @param doc
	 * @param applications
	 */
	private static void readApplicationInfo(Document doc, ArrayList<Application> applications) {
		NodeList nList = doc.getElementsByTagName("application");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String vendorId = eElement.getElementsByTagName("itunes-vendorid").item(0).getTextContent();
				String appleId = eElement.getElementsByTagName("appleid").item(0).getTextContent();
				String applicationId = eElement.getElementsByTagName("ark-applicationid").item(0).getTextContent();
				String type = eElement.getElementsByTagName("item-type").item(0).getTextContent();
				String clearedForSale = eElement.getElementsByTagName("cleared-for-sale").item(0).getTextContent();
				String screenshotPath = eElement.getElementsByTagName("screenshot-path").item(0).getTextContent();

				Application app = new Application (
						vendorId, 
						appleId, 
						applicationId, 
						type, 
						clearedForSale, 
						screenshotPath );

				applications.add(app);
			}		
		}
	}

	/**
	 * Read itmsp file from iTunesConnect, make InAppPurchase object for every
	 * InAppPurchase and put into list 
	 * @return
	 */
	public static ArrayList<InAppPurchase> readITMSFile(Application apps) {
		try {
			Document doc = dBuilder.parse(new File(tempFolder+"/"+ apps.getVendorId() +".itmsp/metadata.xml"));

			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("in_app_purchase");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String productId = eElement.getElementsByTagName("product_id").item(0).getTextContent();
					String referenceName = eElement.getElementsByTagName("reference_name").item(0).getTextContent();

					InAppPurchase iap = new InAppPurchase(
							productId,
							referenceName, 
							0.00);
							//Note: 0.00 is entered as default price for existing InAppPurchases
							
					appsFromITMS.add(iap);
				}		
			}
		} 
		catch (Exception e) {
			logger.catching(e);
		}	
		return appsFromITMS;
	}

	/**
	 * @param hash
	 * @param apps
	 */
	public static void generateXML(ArrayList<InAppPurchase> listFromArk, Application apps) {
		try {
			Document doc = dBuilder.newDocument();

			Element rootElement = doc.createElementNS("http://apple.com/itunes/importer", "package");
			rootElement.setAttribute("version", "software5.2");
			doc.appendChild(rootElement);

			makeTag(doc, "team_id", teamId, rootElement);
			Element software = makeTag(doc, "software", null, rootElement);
			makeTag(doc, "vendor_id", apps.getVendorId(), software);
			Element readOnly = makeTag(doc, "read_only_info", null, software);
			Element appleId = makeTag(doc, "read_only_value", apps.getAppleId(), readOnly);
			appleId.setAttribute("key", "apple-id");
			Element sm = makeTag(doc, "software_metadata", null, software);
			Element inapps = makeTag(doc, "in_app_purchases", null, sm);

			generateForInAppPurchase(listFromArk, inapps, doc, apps);
			printToXML(doc, apps);
		} 
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * @param doc
	 * @param apps
	 */
	private static void printToXML(Document doc, Application apps){
		try {
			transformer = transformerFactory.newTransformer();

			//make pretty
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);

			//overwrites existing metadata.xml
			StreamResult file = new StreamResult(new File( tempFolder + "/" + apps.getVendorId() + ".itmsp" + "/metadata.xml"));
			transformer.transform(source, file);
		}
		catch (Exception e) {
			logger.catching(e);
		}
	}

	/**
	 * Generates tags/elements for InAppPurchases
	 * @param hash
	 * @param parent
	 * @param doc
	 * @param apps
	 */
	private static void generateForInAppPurchase(ArrayList<InAppPurchase> listFromArk, Element parent, Document doc, Application apps) { 

		//Get tabs for every application
		for (int i = 0; i< listFromArk.size(); i++) {
			parent.appendChild(getInApp(doc, apps, listFromArk.get(i)));
		}
	}

	/**
	 * Generates xml for individual InAppPurchases
	 * @param doc
	 * @param apps
	 * @param p
	 * @return
	 */
	private static Node getInApp(Document doc, Application apps, InAppPurchase p) {
		Element in_app = doc.createElement("in_app_purchase");

		makeTag(doc, "product_id", p.getProductId(), in_app);
		makeTag(doc, "reference_name", p.getReferenceName(), in_app);
		makeTag(doc, "type", p.getType(), in_app);
		Element prods = makeTag(doc, "products", null, in_app);  
		Element prod = 	makeTag(doc, "product", null, prods);
		makeTag(doc, "cleared_for_sale", apps.getClearedForSale(), prod);
		Element ins = 	makeTag(doc, "intervals", null, prod);
		Element in =	makeTag(doc, "interval", null, ins);
		makeTag(doc, "wholesale_price_tier",p.getWholesalePriceTier(), in);
		Element locs = 	makeTag(doc, "locales", null, in_app);
		Element loc =	makeTag(doc, "locale", null, locs); 
		loc.setAttribute("name", "en-US");
		makeTag(doc, "title", p.getReferenceName(), loc);
		makeTag(doc, "description", p.getLocaleDescription(p.getReferenceName()), loc);
		//non-consumable cannot have publication name; free-subscription MUST have publication name
		if (p.getType().equals("free-subscription")) 
			makeTag(doc, "publication_name", p.getReferenceName(), loc);
		Element rev = 	makeTag(doc, "review_screenshot", null, in_app);
		makeTag(doc, "size", apps.getScreenshotSize(), rev);
		makeTag(doc, "file_name", apps.getScreenshotName(), rev);
		Element check = makeTag(doc, "checksum", apps.getScreenshotChecksum(), rev);
		check.setAttribute("type", "md5");

		return in_app;
	}

	/**
	 * Generate element tags for xml
	 * @param doc
	 * @param name
	 * @param text
	 * @param parent
	 * @return
	 */
	private static Element makeTag(Document doc, String name, String text, Element parent) {
		Element e = doc.createElement(name);
		if (text != null)
			e.appendChild(doc.createTextNode(text));
		parent.appendChild(e);
		return e;
	}

	/**
	 * List of all currently pending applications to be uploaded
	 * @return
	 */
	public static ArrayList<InAppPurchase> getAppsFromITMS() {
		return appsFromITMS;
	}

	/**
	 * User information, access Ark database
	 * @return
	 */
	public static String getArkUser() {
		return arkUser;
	}

	/**
	 * User information, access Ark database
	 * @return
	 */
	public static String getArkPassword() {
		return arkPassword;
	}

	/**
	 * Ark URL, allows JDBC access
	 * @return
	 */
	public static String getArkUrl() {
		return arkUrl;
	}

	/**
	 * Temporary storage of download files
	 * @return
	 */
	public static String getTempFolder() {
		return tempFolder;
	}

	/**
	 * User access to iTunesConnect
	 * @return
	 */
	public static String getiTunesUser() {
		return iTunesUser;
	}

	/**
	 * User access to iTunesConnect
	 * @return
	 */
	public static String getiTunesPassword() {
		return iTunesPassword;
	}

	/**
	 * Team ID for Application
	 * @return
	 */
	public static String getTeamId() {
		return teamId;
	}

	/**
	 * Transporter location will vary with machine
	 * @return
	 */
	public static String getPathToTransporter() {
		return pathToTransporter;
	}
}
