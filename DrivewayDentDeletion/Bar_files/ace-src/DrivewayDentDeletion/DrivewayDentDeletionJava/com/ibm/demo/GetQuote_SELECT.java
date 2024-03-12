package com.ibm.demo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbJSON;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class GetQuote_SELECT extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below			
			MbElement LocalEnv = inAssembly.getLocalEnvironment().getRootElement();			
			MbElement WhereQuoteID = LocalEnv.getFirstElementByPath("/REST/Input/Parameters/QuoteID");
			MbElement WhereName = LocalEnv.getFirstElementByPath("/REST/Input/Parameters/Name");
			MbElement WhereEmail = LocalEnv.getFirstElementByPath("/REST/Input/Parameters/Email");						
			MbElement outputJsonRoot = outMessage.getRootElement().createElementAsLastChild(MbJSON.PARSER_NAME);
			MbElement outputJsonData = outputJsonRoot.createElementAsLastChild(MbJSON.ARRAY,MbJSON.DATA_ELEMENT_NAME, null);
			Connection conn = getJDBCType4Connection("{DefaultPolicies}:PostgresqlPolicy", JDBC_TransactionType.MB_TRANSACTION_AUTO);
			Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			if (WhereQuoteID!=null) {				
				ResultSet rs = stmt.executeQuery("SELECT * FROM QUOTES WHERE QuoteID="+WhereQuoteID.getValueAsString());
				createResponse(rs, outputJsonData);
			} else if (WhereEmail!=null) {
				// Add LIMIT 5 just in case we end up with 100s or 1000s of table entries!
				ResultSet rs = stmt.executeQuery("SELECT * FROM QUOTES WHERE Email='"+WhereEmail.getValueAsString()+"' LIMIT 5");
				createResponse(rs, outputJsonData);				
			} else if (WhereName!=null)	{	
				// Add LIMIT 5 just in case we end up with 100s or 1000s of table entries!
				ResultSet rs = stmt.executeQuery("SELECT * FROM QUOTES WHERE Name='"+WhereName.getValueAsString()+"' LIMIT 5");
				createResponse(rs, outputJsonData);
			} else {
				outputJsonData.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Error", "Cannot locate a quote due to not finding one of QuoteID, Email or Name parameter in the request.");
			}
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}

	
	public void createResponse(ResultSet rs, MbElement outputJsonData) throws Exception {	
		while (rs.next()) {					
			MbElement QuoteCurrentArrayItem = outputJsonData.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.ARRAY_ITEM_NAME, null);					
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "QuoteID", rs.getString("quoteid"));
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Name", rs.getString("name"));
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Email", rs.getString("email"));
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Address", rs.getString("address"));
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "USState", rs.getString("usstate"));
			QuoteCurrentArrayItem.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "LicensePlate", rs.getString("licenseplate"));
			MbElement RepairQuotes = QuoteCurrentArrayItem.createElementAsLastChild(MbJSON.ARRAY, "RepairQuotes", null);
			MbElement RepairQuoteArrayItemAcme = RepairQuotes.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.ARRAY_ITEM_NAME, null);
			RepairQuoteArrayItemAcme.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "VendorName", "AcmeAutoAccidents");
			RepairQuoteArrayItemAcme.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "CostEstimate", rs.getString("acmecost"));
			RepairQuoteArrayItemAcme.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "EarliestAppointmentDate", rs.getString("acmedate"));
			MbElement RepairQuoteArrayItemBernie = RepairQuotes.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.ARRAY_ITEM_NAME, null);
			RepairQuoteArrayItemBernie.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "VendorName", "BernieBashedBumpers");
			RepairQuoteArrayItemBernie.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "CostEstimate", rs.getString("berniecost"));
			RepairQuoteArrayItemBernie.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "EarliestAppointmentDate", rs.getString("berniedate"));
			MbElement RepairQuoteArrayItemChris = RepairQuotes.createElementAsLastChild(MbElement.TYPE_NAME, MbJSON.ARRAY_ITEM_NAME, null);
			RepairQuoteArrayItemChris.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "VendorName", "ChrisCrumpledCars");
			RepairQuoteArrayItemChris.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "CostEstimate", rs.getString("chriscost"));
			RepairQuoteArrayItemChris.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "EarliestAppointmentDate", rs.getString("chrisdate"));
			
			}
	}
	
	
	/**
	 * onPreSetupValidation() is called during the construction of the node
	 * to allow the node configuration to be validated.  Updating the node
	 * configuration or connecting to external resources should be avoided.
	 *
	 * @throws MbException
	 */
	@Override
	public void onPreSetupValidation() throws MbException {
	}

	/**
	 * onSetup() is called during the start of the message flow allowing
	 * configuration to be read/cached, and endpoints to be registered.
	 *
	 * Calling getPolicy() within this method to retrieve a policy links this
	 * node to the policy. If the policy is subsequently redeployed the message
	 * flow will be torn down and reinitialized to it's state prior to the policy
	 * redeploy.
	 *
	 * @throws MbException
	 */
	@Override
	public void onSetup() throws MbException {
	}

	/**
	 * onStart() is called as the message flow is started. The thread pool for
	 * the message flow is running when this method is invoked.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStart() throws MbException {
	}

	/**
	 * onStop() is called as the message flow is stopped. 
	 *
	 * The onStop method is called twice as a message flow is stopped. Initially
	 * with a 'wait' value of false and subsequently with a 'wait' value of true.
	 * Blocking operations should be avoided during the initial call. All thread
	 * pools and external connections should be stopped by the completion of the
	 * second call.
	 *
	 * @throws MbException
	 */
	@Override
	public void onStop(boolean wait) throws MbException {
	}

	/**
	 * onTearDown() is called to allow any cached data to be released and any
	 * endpoints to be deregistered.
	 *
	 * @throws MbException
	 */
	@Override
	public void onTearDown() throws MbException {
	}

}
