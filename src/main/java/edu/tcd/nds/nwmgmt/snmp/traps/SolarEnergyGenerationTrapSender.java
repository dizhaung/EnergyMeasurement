package edu.tcd.nds.nwmgmt.snmp.traps;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import edu.tcd.nds.nwmgmt.utils.Constants;

/**
 * The implementation of solar energy generation trap sender. The basic
 * responsibility of this class is to send trap using {@link Snmp} API. This
 * class uses localhost and 2002 port for sending PDU notification. Therefore,
 * make sure that host and port combination is not already in use.
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class SolarEnergyGenerationTrapSender {
	public static final String SOLAR_ENERGY_GENERATION_TRAP_HOST = "127.0.0.1";
	public static final String SOLAR_ENERGY_GENERATION_TRAP_PORT = "2002";

	private Snmp snmp;
	
	public void setSnmp(Snmp snmp){
		this.snmp = snmp;
	}

	/**
	 * This methods sends the V1 trap to the Localhost in port
	 * SolarEnergyGenerationTrapReceiver.SOLAR_ENERGY_GENERATION_TRAP_PORT
	 */
	public void sendSolarEnergyGenerationTrap(OID oid, String value) {
		try {
			// Create Target
			CommunityTarget cTarget = new CommunityTarget();
			cTarget.setCommunity(new OctetString(Constants.COMMUNITY));
			cTarget.setVersion(SnmpConstants.version2c);
			cTarget.setAddress(new UdpAddress(SOLAR_ENERGY_GENERATION_TRAP_HOST + "/" + SOLAR_ENERGY_GENERATION_TRAP_PORT));
			cTarget.setRetries(2);
			cTarget.setTimeout(5000);

			// Create PDU for V2
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(oid, new OctetString(value)));
			pdu.setType(PDU.NOTIFICATION);

			// Send the PDU
			System.out.println("Sending V2 Trap... ");
			snmp.send(pdu, cTarget);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}