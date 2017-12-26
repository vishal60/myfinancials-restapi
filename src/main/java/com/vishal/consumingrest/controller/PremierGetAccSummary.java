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
import com.vishal.consumingrest.entities.xsdclasses.AccountSiteSummary;
import com.vishal.consumingrest.entities.xsdclasses.Row;
import com.vishal.consumingrest.entities.ParentParty;

@RestController
public class PremierGetAccSummary {

    @RequestMapping("/premier_accounts/getaccsummary")
    public AccountSiteSummary xml2Json(@RequestParam(value="account_id") String account_id,
							    	   @RequestParam(value="address_id") String address_id,
							    	   @RequestParam(value="email") String email,
							    	   @RequestParam(value="parent_party_id") String parent_party_id) {
    	
		StringBuffer sbPayload = getPayloadfromHttp("https://otc-dev-vm.us.dell.com:17658/premier_accounts/getaccsummary", account_id, address_id, email, parent_party_id);
		String xmlBody = removeSoapTags(sbPayload).trim();
		AccountSiteSummary accountSiteSummary = unMarshall(xmlBody);
    	
    	return accountSiteSummary;
    }

	private AccountSiteSummary unMarshall(String xmlBody) {
		
		AccountSiteSummary accountSiteSummary = null;
		try {
			Unmarshaller unmarshaller = JAXBContext.newInstance(AccountSiteSummary.class).createUnmarshaller();
			accountSiteSummary = (AccountSiteSummary)unmarshaller.unmarshal(new StringReader(xmlBody));
		} catch (JAXBException e) {e.printStackTrace();}
		return accountSiteSummary;
	}
   
	private StringBuffer getPayloadfromHttp(String urlString, String account_id, String address_id, String email, String parent_party_id) {

		StringBuffer sbPayload = new StringBuffer();

		try {
			URL url = new URL(urlString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/xml");
			
			OutputStream os = conn.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");    
			osw.write(getUser(account_id, address_id, email, parent_party_id));
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

	private static String removeSoapTags(StringBuffer sb) {
		
		JSONObject jsonObject = xml2json(sb);
		return XML.toString( ((JSONObject)((JSONObject)((JSONObject)((JSONObject)jsonObject.get("soapenv:Envelope")).get("soapenv:Body")))) );
		
	}
	
	private static String getUser(String account_id, String address_id, String email, String parent_party_id
) {
		return "<?xml version='1.0' encoding='utf-8'?>"+
		"<SOAP-ENV:Envelope xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/'>"+
		"<SOAP-ENV:Body>" +
		"	<request_parameters>" +
		"		<account_id>" + account_id + "</account_id>" +
		"		<address_id>" + address_id + "</address_id>" +
		"		<email>" + email + "</email>" +
		"		<parent_party_id>" + parent_party_id + "</parent_party_id>" +
		"		<outstanding_criteria>1</outstanding_criteria>" +
		"	</request_parameters></SOAP-ENV:Body>" +
		"</SOAP-ENV:Envelope>";
	}

	private static JSONObject xml2json(StringBuffer sb) {
		
		JSONObject jsonObject = XML.toJSONObject(sb.toString());
		return jsonObject;
	}
	
}
