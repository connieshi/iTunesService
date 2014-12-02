package com.timeinc.mageng.arkdistributor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Connie Shi
 * Get records from Ark to insert into iTunesConnect
 * 
 */
public class ArkDAO {
	static final Logger logger = LogManager.getLogger(ArkDAO.class);
	private ArrayList<InAppPurchase> listFromArk = new ArrayList<InAppPurchase>();
	
	/**
	 * Each ArkDAO instance queries into Ark with JDBC
	 * @param sqlUser
	 * @param sqlPassword
	 * @param apps
	 */
	public ArkDAO(String sqlUser, String sqlPassword, Application apps){
		getIssuesFromArk(sqlUser, sqlPassword, apps);
	}

	/**
	 * Gets records from Ark, put InAppPurchase objects in list
	 * @param username
	 * @param password
	 * @param apps
	 * @return
	 */
	private void getIssuesFromArk(String sqlUsername, String sqlPassword, Application apps) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection con = DriverManager.getConnection(
					"jdbc:mysql:"+ ITMSConfiguration.getArkUrl(),
					sqlUsername, sqlPassword);

			Statement stmt = con.createStatement();

			ResultSet rs = stmt.executeQuery(
					" SELECT im.ReferenceId, im.price, i.name "
							+ 	" FROM issue_meta im "
							+ 	" JOIN issues i ON i.id = im.IssueId "
							+ 	" WHERE im.ApplicationId = "	+ apps.getApplicationId() 
							+	" AND im.onsale_date > curdate();" ); //Ensures future issues

			while (rs.next()) {
				String referenceID = rs.getString("ReferenceId");
				double price = rs.getDouble("price");
				String name = rs.getString("name");

				InAppPurchase i = new InAppPurchase( 
						referenceID, 
						name, 
						price); 

				listFromArk.add(i);
			}
		}
		catch (Exception e) {
			logger.catching(e);
		} 
	}

	/**
	 * List of all In App Purchases with release date after current date
	 * @return
	 */
	public ArrayList<InAppPurchase> getListFromArk() {
		return listFromArk;
	}
}
