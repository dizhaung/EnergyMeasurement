package edu.tcd.nds.nwmgmt.snmp;

import java.io.File;
import java.io.IOException;

import javax.management.InvalidAttributeValueException;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.MOTableRow;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.transport.TransportMappings;

import edu.tcd.nds.nwmgmt.models.Apartment;
import edu.tcd.nds.nwmgmt.models.Flat;
import edu.tcd.nds.nwmgmt.snmp.traps.SolarEnergyGenerationTrapReceiver;
import edu.tcd.nds.nwmgmt.utils.Constants;

/**
 * The implementation of Agent class using SNMP4J library. To run this class
 * pass-in host and port in host/port fashion. This class is responsible for
 * creation of model objects and setting default values to model object's
 * managed objects.
 * 
 * It trigger registration of managed object which are then used to getting and
 * setting values by manager or client. The actual registration is handled by
 * model object.
 * 
 * This class also creation security view that is used to grant access to
 * managed objects. As of now it gives very basic security access, i.e. write
 * access to all managed object except devide ID.
 * 
 * The trap receivers are registered here which run in separate thread. The
 * spawned new thread has wait infinitely and dies along with the agent.
 * Whenever a managed object value is changed and a notification trap is fired
 * then the receiving trap gets callback. The callback method then manipulate
 * other managed object parameters
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class EnergyMeasurementAgent extends BaseAgent {

	private String address;
	Apartment appartment = null;
	
	public EnergyMeasurementAgent(String address) throws IOException {
		super(new File("conf.agent"), new File("bootCounter.agent"), 
				new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
		this.address = address;
	}
	
	protected void registerTraps(){
		SolarEnergyGenerationTrapReceiver solarEnergyGenerationTrapReceiver = new SolarEnergyGenerationTrapReceiver(appartment);
		Thread trap = new Thread(solarEnergyGenerationTrapReceiver);
		trap.start();
	}

	/**
	 * We let clients of this agent register the MO they
	 * need so this method does nothing
	 */
	@Override
	protected void registerManagedObjects() {
		appartment = new Apartment();
		appartment.setDeviceIdMOValue("62TerenureEast");
		appartment.setConsumptionMOValue("150");
		appartment.setStorageMOValue("10");
		appartment.setGenerationByHydroMOValue("120");
		appartment.setGenerationBySolarMOValue("20");
		
		for (int i = 1; i < 6; i++) {
			Flat flat = new Flat();
			flat.setDeviceIdValue("FlatNo_"+i);
			flat.setEnergyConsumptionMOValue("30");
			flat.setEnergyConsumptionByHeatingCoolingMOValue("15");			
			flat.setEnergyConsumptionByLightingMOValue("5");
			flat.setEnergyConsumptionByMiscMOValue("10");
			appartment.addFlat(flat);
		}
		
		try{
			appartment.registerMOs(this);
		} catch (InvalidAttributeValueException ex){
			ex.printStackTrace();
		} catch (DuplicateRegistrationException ex){
			ex.printStackTrace();
		}
	}

	/*
	 * Empty implementation
	 */
	@Override
	protected void addNotificationTargets(SnmpTargetMIB targetMIB,
			SnmpNotificationMIB notificationMIB) {
	}
	

	/**
	 * Minimal View based Access Control
	 * 
	 * http://www.faqs.org/rfcs/rfc2575.html
	 */
	@Override
	protected void addViews(VacmMIB vacm) {

		vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, //  the security model. 
				new OctetString("cpublic"), //  the security name. 
				new OctetString("v1v2group"), //  the group name.
				StorageType.nonVolatile); // the storage type for the new entry.

		vacm.addAccess(new OctetString("v1v2group"), // group name 
				new OctetString(Constants.COMMUNITY), //context prefix
				SecurityModel.SECURITY_MODEL_ANY, // security model 
				SecurityLevel.NOAUTH_NOPRIV, // security level
				MutableVACM.VACM_MATCH_EXACT, // using exact match 
				new OctetString("fullReadView"), //  the view name for read access 
				new OctetString("fullWriteView"), //  the view name for write access  
				new OctetString("fullNotifyView"), // the view name for notify access
				StorageType.nonVolatile); // the StorageType for this access entry.

		vacm.addViewTreeFamily(new OctetString("fullReadView"), // the view name.
				new OID("1.3"), // the subtree OID.
				new OctetString(), 
				VacmMIB.vacmViewIncluded,//  indicates whether the view defined by subtree and mask is included or excluded, here included
				StorageType.nonVolatile); // the StorageType for this access entry.
		
		vacm.addViewTreeFamily(new OctetString("fullWriteView"), // the view name.
				new OID("1.3"), // the subtree OID.
				new OctetString(), 
				VacmMIB.vacmViewIncluded,//  indicates whether the view defined by subtree and mask is included or excluded, here included
				StorageType.nonVolatile); // the StorageType for this access entry.
	}
	
	/**
	 * Start method invokes some initialization methods needed to
	 * start the agent
	 * @throws IOException
	 */
	public void start() throws IOException {

		init();
		// This method reads some old config from a file and causes
		// unexpected behavior.
		// loadConfig(ImportModes.REPLACE_CREATE); 
		addShutdownHook();
		getServer().addContext(new OctetString(Constants.COMMUNITY));
		finishInit();
		run();
		sendColdStartNotification();
		registerTraps();
	}
	
	
	/**
	 * Clients can register the MO they need
	 */
	public void registerManagedObject(ManagedObject mo) {
		try {
			server.register(mo, null);
		} catch (DuplicateRegistrationException ex) {
			throw new RuntimeException(ex);
		}
	}
	public void unregisterManagedObject(MOGroup moGroup) {
		moGroup.unregisterMOs(server, getContext(moGroup));
	}	
	protected void unregisterManagedObjects() {
		// here we should unregister those objects previously registered...
	}
	/**
	 * User based Security Model, only applicable to
	 * SNMP v.3
	 * 
	 */
	protected void addUsmUser(USM usm) {
	}
	
	//overwrite
	protected void initTransportMappings() throws IOException {
		transportMappings = new TransportMapping[1];
		Address udpAddress = GenericAddress.parse(address);
		TransportMapping tm = TransportMappings.getInstance().createTransportMapping(udpAddress);
		transportMappings[0] = tm;
	}


	/**
	 * The table of community strings configured in the SNMP
	 * engine's Local Configuration Datastore (LCD).
	 * 
	 * We only configure one, "public".
	 */
	protected void addCommunities(SnmpCommunityMIB communityMIB) {
		Variable[] com2sec = new Variable[] { 
				new OctetString(Constants.COMMUNITY), // community name
				new OctetString("cpublic"), // security name
				getAgent().getContextEngineID(), // local engine ID
				new OctetString(Constants.COMMUNITY), // default context name
				new OctetString(), // transport tag
				new Integer32(StorageType.nonVolatile), // storage type
				new Integer32(RowStatus.active) // row status
		};
		MOTableRow row = communityMIB.getSnmpCommunityEntry().createRow(new OctetString("public2public").toSubIndex(true), com2sec);
		communityMIB.getSnmpCommunityEntry().addRow(row);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if(args.length != 1){
			System.out.println("Usage: java EnergyMeasurementAgent <private_ip_address/port>");
			return;
		}
		String ipAndPort = args[0];
		
		EnergyMeasurementAgent agent = new EnergyMeasurementAgent(ipAndPort);
		agent.start();
		System.out.println("Agent running...");
		while(true) {			
			Thread.sleep(5000);
		}
	}
}
