package com.vishal.consumingrest.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vishal.consumingrest.entities.Address;
import com.vishal.consumingrest.entities.UserAccount;
import com.vishal.consumingrest.entities.xsdclasses.Row;
import com.vishal.consumingrest.entities.ParentParty;

@RestController
public class PremierGetAccountsController {

    @RequestMapping("/premier_accounts/getaccounts")
    public Row /*UserAccount*/ xml2Json(@RequestParam(value="user_name") String user_name,
    		@RequestParam(value="account_number") String account_number) {
    	
		StringBuffer sbPayload = getPayloadfromHttp("https://otc-dev-vm.us.dell.com:17658/premier_accounts/getaccounts", user_name, account_number);
		
		/*CASE 1: UNCOMMENT BELOW IF USING X2J TOOL FOR GENERATING JAVA SRC FILES AND USE (Row) ABOVE*/
		String userDetails = findByAccountNumber(sbPayload, account_number);	
		
		/*CASE 2: UNCOMMENT BELOW IF NOT USING X2J TOOL FOR GENERATING JAVA SRC FILES AND USE (UserAccount) ABOVE*/
		/*JSONObject userDetails = findByAccountNumber2(sbPayload, account_number);		
		UserAccount user = toGreeting(userDetails);
		System.out.println(user);	
        return user;*/
		
		Row userAccount = toUserAccount(userDetails);
		
		return userAccount;
    }

    
    private Row toUserAccount(String userDetails) {

    	Row userAccount = null;
    	
    	try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(Row.class).createUnmarshaller();
			userAccount = (Row)unmarshaller.unmarshal(new StringReader(userDetails));
			//
		} catch (JAXBException e) {e.printStackTrace();}
    	
    	return userAccount;
	}

	private UserAccount toGreeting(JSONObject userDetails) {

	UserAccount user = new UserAccount();
	Address address = new Address();
	ParentParty parentParty = new ParentParty();
	
	user.setAccount_name(userDetails.get("account_name").toString());
	user.setAccount_number(userDetails.get("account_number").toString());
	user.setCurrency_code(userDetails.get("currency_code").toString());
	user.setCust_account_id(userDetails.get("cust_account_id").toString());
	user.setOutstanding_balance(userDetails.get("outstanding_balance").toString());
	user.setParty_id(userDetails.get("party_id").toString());
	user.setParty_number(userDetails.get("party_number").toString());
	
	if (userDetails.get("parent_party") != "") {
		parentParty.setParty_id(((JSONObject) ((JSONObject) userDetails.get("parent_party")).get("parent_party_row"))
						.get("party_id").toString());
		parentParty.setParty_number(((JSONObject) ((JSONObject) userDetails.get("parent_party")).get("parent_party_row"))
						.get("party_number").toString());
		user.setParent_party(parentParty);
	} else 
		user.setParent_party(null);
	
	if (userDetails.get("address") != "") {
		JSONObject jAddress = (JSONObject) ((JSONObject) userDetails.get("address")).get("address_row");
		address.setAddress_id(jAddress.get("address_id").toString());
		address.setBranch_name(((JSONObject) jAddress.get("branch_name")).get("null").toString());
		address.setConcatenated_address(jAddress.get("concatenated_address").toString());
		address.setFraud_bankruptcy(jAddress.get("fraud_bankruptcy").toString());
		user.setAddresses(address);
	} else
		user.setAddresses(null);
	return user;
}
    
	private StringBuffer getPayloadfromHttp(String urlString, String user_name, String account_number) {

		StringBuffer sbPayload = new StringBuffer();

		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/xml");
			
			OutputStream os = conn.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
			osw.write(getUser(user_name));
			osw.flush();
			osw.close();			
			os.flush();

			BufferedReader msg = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String currentLine = msg.readLine();
			while(currentLine != null){
				sbPayload.append(currentLine);
				currentLine = msg.readLine();
			}
			
			conn.disconnect();
			
		} catch (MalformedURLException e) {
			System.out.println("URL exception");
		} catch (IOException e) {
			System.out.println("http url opening error");
		}
		return sbPayload;
	}

	private static String findByAccountNumber(StringBuffer sb, String account_number) {
		
		JSONObject jsonObject = xml2json(sb);
		JSONArray jsonArray = (JSONArray) ((JSONObject)((JSONObject)((JSONObject)jsonObject.get("soapenv:Envelope")).get("soapenv:Body")).get("accounts_list")).get("row");
		
		for(Object obj: jsonArray){
			Object accountNumber = ((JSONObject)obj).get("account_number");
			if(accountNumber.toString().equalsIgnoreCase(account_number)) 			
				return "<row>" + XML.toString((JSONObject)obj) + "</row>";
		}
		return "{'error msg':'No data available for the given account number'}";
	}
	
	private static JSONObject findByAccountNumber2(StringBuffer sb, String account_number) {
		JSONObject jsonObject = xml2json(sb);
		JSONArray jsonArray = (JSONArray) ((JSONObject)((JSONObject)((JSONObject)jsonObject.get("soapenv:Envelope")).get("soapenv:Body")).get("accounts_list")).get("row");
		
		for(Object obj: jsonArray){
			Object accountNumber = ((JSONObject)obj).get("account_number");
			if(accountNumber.toString().equalsIgnoreCase(account_number)) return (JSONObject) obj;
		}
		return new JSONObject("{'error msg':'No data available for the given account number'}");
	}

	private static String getUser(String userName) {
		return "<?xml version='1.0' encoding='utf-8'?>"+
		"<SOAP-ENV:Envelope xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/'>"+
		"<SOAP-ENV:Body>" +
		"	<request_parameters>" +
		"		<email>" + userName + "</email>" +
		"		<outstanding_criteria>1</outstanding_criteria>" +
		"	</request_parameters></SOAP-ENV:Body>" +
		"</SOAP-ENV:Envelope>";
	}

	private static JSONObject xml2json(StringBuffer sb) {
		
		JSONObject jsonObject = XML.toJSONObject(sb.toString());
		//String jsondata = jsonObject.toString(2);
		return jsonObject;
	}
	
}
