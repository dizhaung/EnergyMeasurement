package edu.tcd.nds.nwmgmt.snmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import edu.tcd.nds.nwmgmt.snmp.traps.SolarEnergyGenerationTrapSender;
import edu.tcd.nds.nwmgmt.utils.Constants;
import edu.tcd.nds.nwmgmt.utils.MOIdentifiers;

/**
 * The manager class that uses Snmp API to set and get values of managed
 * objects. This class runs on same host and port as Agent however it just
 * listen to that port and does not bind like agent class.
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class EnergyMeasurementManager {

	private String address;
	private Snmp snmp;
	

	/**
	 * Construct the manager object with host and port given in parameter
	 * 
	 * @param address
	 *            the host and port where this manager object will listen
	 */
	public EnergyMeasurementManager(String address) {
		this.address = address;
		try {
			start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Since snmp4j relies on asynch req/resp we need a listener for responses
	 * which should be closed
	 * 
	 * @throws IOException throws {@link IOException} if IO operation fail
	 */
	public void stop() throws IOException {
		snmp.close();
	}

	/**
	 * Start to listen to host and port specified in constructor. The manager
	 * just listen and does not bind to given address.
	 * 
	 * @throws IOException
	 *             if anything goes wrong while performing IO operation
	 */
	private void start() throws IOException {
		TransportMapping transport = new DefaultUdpTransportMapping();
		snmp = new Snmp(transport);
		transport.listen();
	}
	
	/**
	 * Return the value of managed object as String. The OID will decide which
	 * managed object to look.
	 * 
	 * @param oid
	 *            the managed object to look for
	 * @return the value of managed object
	 * @throws IOException
	 *             if anything goes wrong while performing IO operation
	 */
	public String getAsString(OID oid) throws IOException {
		ResponseEvent event = get(new OID[]{oid});
		return event.getResponse().get(0).getVariable().toString();
	}

	/**
	 * This method help in setting value of given managed object.
	 * 
	 * @param oid
	 *            the managed object whoes value to set
	 * @param value
	 *            the value of managed object
	 * @throws IOException
	 *             if anything goes wrong while performing IO operation
	 */
	public void setAsString(OID oid, String value) throws IOException {
		PDU pdu = new PDU();
		VariableBinding inputParam = new VariableBinding(oid);
		inputParam.setVariable(new OctetString(value));
		pdu.add(inputParam);
		pdu.setType(PDU.SET);
		ResponseEvent event = snmp.send(pdu, getTarget(), null);
		System.out.println("event.getResponse() "+event.getResponse());
	}

	/**
	 * Return the value of managed object as String. This method is called in
	 * async mode and callback listener is hooked where response will be send.
	 * The OID will decide which managed object to look.
	 * 
	 * @param oid
	 *            the managed object to look for
	 * @return the value of managed object
	 * @throws IOException
	 *             if anything goes wrong while performing IO operation
	 */
	public void getAsString(OID oids,ResponseListener listener) {
		try {
			snmp.send(getPDU(new OID[]{oids}), getTarget(),null, listener);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	private PDU getPDU(OID oids[]) {
		PDU pdu = new PDU();
		for (OID oid : oids) {
			pdu.add(new VariableBinding(oid));
		}
	 	   
		pdu.setType(PDU.GET);
		return pdu;
	}
	
	public ResponseEvent get(OID oids[]) throws IOException {
	   ResponseEvent event = snmp.send(getPDU(oids), getTarget(), null);
	   if(event != null) {
		   return event;
	   }
	   throw new RuntimeException("GET timed out");	  
	}
	
	private Target getTarget() {
		Address targetAddress = GenericAddress.parse(address);
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(Constants.COMMUNITY));
		target.setAddress(targetAddress);
		target.setRetries(2);
		target.setTimeout(1500);
		target.setVersion(SnmpConstants.version2c);
		return target;
	}

	/**
	 * Normally this would return domain objects or something else than this...
	 */
	public List<List<String>> getTableAsStrings(OID[] oids) {
		TableUtils tUtils = new TableUtils(snmp, new DefaultPDUFactory());
		
		@SuppressWarnings("unchecked") 
			List<TableEvent> events = tUtils.getTable(getTarget(), oids, null, null);
		
		List<List<String>> list = new ArrayList<List<String>>();
		for (TableEvent event : events) {
			if(event.isError()) {
				throw new RuntimeException(event.getErrorMessage());
			}
			List<String> strList = new ArrayList<String>();
			list.add(strList);
			for(VariableBinding vb: event.getColumns()) {
				strList.add(vb.getVariable().toString());
			}
		}
		return list;
	}
	
	/**
	 * A trap sender for solar energy generation managed object. This method
	 * should be called to send a trap when solar energy generation value is
	 * changed.
	 * 
	 * @param oid
	 *            the solar energy generation managed object OID. Please refer
	 *            {@link MOIdentifiers} for more details
	 * @param value
	 *            the value of managed object to be set. As the trap receiver is
	 *            handled by agent which has access to all updated values of
	 *            managed object, this value need not to be accurate however
	 *            should not be null as well
	 */
	public void sendSolarEnergyGenerationTrap(OID oid, String value){
		SolarEnergyGenerationTrapSender sender = new SolarEnergyGenerationTrapSender();
		sender.setSnmp(snmp);
		sender.sendSolarEnergyGenerationTrap(oid, value);
	}
	
	public static String extractSingleString(ResponseEvent event) {
		return event.getResponse().get(0).getVariable().toString();
	}
}
