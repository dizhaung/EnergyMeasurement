package edu.tcd.nds.nwmgmt.snmp.traps;

import java.io.IOException;
import java.util.Vector;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import edu.tcd.nds.nwmgmt.models.Apartment;
import edu.tcd.nds.nwmgmt.utils.Constants;
import edu.tcd.nds.nwmgmt.utils.MOIdentifiers;

/**
 * The solar energy generation trap receiver. This receiver is managed by agent
 * and has full access to model object. Therefore use it wisely to update any
 * managed object using model object.
 *
 * This trap run on same host where agent is running however the port is
 * different. This is because this trap listen to given port and goes in wait
 * mode till the agent dies. Making use of same port as agent would have halt
 * execution of agent. That is why the trap is run in separate thread then agent
 * as well.
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class SolarEnergyGenerationTrapReceiver implements CommandResponder, Runnable {
	public static final String SOLAR_ENERGY_GENERATION_TRAP_PORT = "2002";
	public static final String SOLAR_ENERGY_GENERATION_TRAP_HOST = "localhost";
	private Apartment apartment;
	
	/**
	 * Default constructor
	 */
	public SolarEnergyGenerationTrapReceiver(){
		// default constructor
	}
	
	/**
	 * Construct a trap receiver which {@link Apartment} model object.
	 * 
	 * @param apartment
	 *            the {@link Apartment} model object
	 */
	public SolarEnergyGenerationTrapReceiver(Apartment apartment){
		this.apartment = apartment;
	}
	
	public void run(){
		try {
			listen(new UdpAddress(SOLAR_ENERGY_GENERATION_TRAP_HOST+"/"+SOLAR_ENERGY_GENERATION_TRAP_PORT));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Trap listener
	 */
	public synchronized void listen(TransportIpAddress address) throws IOException {
		AbstractTransportMapping transport;
		if (address instanceof TcpAddress) {
			transport = new DefaultTcpTransportMapping((TcpAddress) address);
		} else {
			transport = new DefaultUdpTransportMapping((UdpAddress) address);
		}

		ThreadPool threadPool = ThreadPool.create("DispatcherPool", 5);
		MessageDispatcher mDispathcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

		// add message processing models
		mDispathcher.addMessageProcessingModel(new MPv1());
		mDispathcher.addMessageProcessingModel(new MPv2c());

		// add all security protocols
		SecurityProtocols.getInstance().addDefaultProtocols();
		SecurityProtocols.getInstance().addPrivacyProtocol(new Priv3DES());

		// Create Target
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(Constants.COMMUNITY));

		Snmp snmp = new Snmp(mDispathcher, transport);
		snmp.addCommandResponder(this);

		transport.listen();
		System.out.println("Listening on " + address);

		try {
			this.wait();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * This method will be called whenever a pdu is received on the given port
	 * specified in the listen() method
	 */
	public synchronized void processPdu(CommandResponderEvent cmdRespEvent) {
		System.out.println("Received PDU for SolarEnergyGenerationTrap handler...");
		PDU pdu = cmdRespEvent.getPDU();
		if (pdu != null) {
			Vector<VariableBinding> bindings = pdu.getVariableBindings();
			VariableBinding binding = bindings.get(0);
			OID oid = binding.getOid(); 
			if(oid.equals(new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_SOLAR))){
				adjustEnergyGenerationMOValues();
			} else {
				System.out.println("Some other OID. Trap notification is not for solar energy generation, skipping.");
			}
		}
	}
	
	/**
	 * According to scenario update values of other managed objects.
	 */
	private void adjustEnergyGenerationMOValues(){
		String generatedBySolarStr = apartment.getEnergyGenerationBySolarMO().getValue().toString();
		String generatedByHydroStr = apartment.getEnergyGenerationByHydroMO().getValue().toString();
		String energyStorageStr = apartment.getEnergyStorageMO().getValue().toString();
		String totalEnergyGenerationStr = apartment.getEnergyGenerationMO().getValue().toString();
		
		int generatedBySolarInt = 0;
		int generationByHydroInt = 0;
		int energyStorageInt = 0;
		int totalEnergyGenerationInt = 0;

		// convert string to int
		try{
			generatedBySolarInt = Integer.parseInt(generatedBySolarStr);
			generationByHydroInt = Integer.parseInt(generatedByHydroStr);
			energyStorageInt = Integer.parseInt(energyStorageStr);
			totalEnergyGenerationInt = Integer.parseInt(totalEnergyGenerationStr);
		} catch (NumberFormatException ex){
			System.out.println("The value set for energy generated by hydro, solar or storage capacity is not a integer.");
		}
		
		int energySurplus = (generatedBySolarInt + generationByHydroInt + energyStorageInt) - totalEnergyGenerationInt;
		float energySurplusSign = Math.signum(energySurplus);
		if(energySurplusSign == 1.0) { // total energy generated is in surplus
			System.out.println("energy generated by all sources is in surplus than what is needed.");
			int energySurplusAfterStorage = energySurplus - energyStorageInt;
			float energySurplusAfterStorageSign = Math.signum(energySurplusAfterStorage);
			if(energySurplusAfterStorageSign == 1.0){ // energy generated by solar is in surplus
				System.out.println("energy generated by solar can be used to replace energy utilization from storage.");
				apartment.setStorageMOValue("0");
				apartment.setGenerationByHydroMOValue(""+(generationByHydroInt-energySurplusAfterStorage));
			} else if(energySurplusSign == 0){ // all good, no changes needed.
				// do nothing
				System.out.println("energy generated by all sources is exactly what is needed.");
			}
		} else if(energySurplusSign == -1.0){ // total energy generated is in dearth 
			System.out.println("energy generated by all sources is in dearth than what is needed.");
			energyStorageInt = 10; // update storage
			generationByHydroInt = totalEnergyGenerationInt - (energyStorageInt + generatedBySolarInt); //update hydro 
			apartment.setStorageMOValue(""+energyStorageInt);
			apartment.setGenerationByHydroMOValue(""+generationByHydroInt);
		} else if(energySurplusSign == 0){ // all good, no changes needed.
			// do nothing
			System.out.println("energy generated by all sources is exactly what is needed.");
		}
	}
}