package edu.tcd.nds.nwmgmt.models;

import java.util.ArrayList;
import java.util.List;

import javax.management.InvalidAttributeValueException;

import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.SMIConstants;

import edu.tcd.nds.nwmgmt.utils.MOIdentifiers;
import edu.tcd.nds.nwmgmt.utils.MOTableBuilder;

/**
 * A model class that represents apartment in actual usecase defined by Group2
 * energy simulation for smart district project. Please check usecase in
 * documentation
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class Apartment extends BaseModel {
	private MOScalar deviceIdMO;
	private MOScalar energyConsumptionMO;
	private MOScalar energyStorageMO;
	private MOScalar energyGenerationMO;
	private MOScalar energyGenerationBySolarMO;
	private MOScalar energyGenerationByHydroMO;
	

	private MOTableBuilder builder; 
	private List<Flat> flats;
	
	/**
	 * Build a Apartment model object and initialize managed object that are
	 * handled by this model object.
	 */
	public Apartment(){
		deviceIdMO = new MOScalar(MOIdentifiers.APPT_IDENTIFIER, MOAccessImpl.ACCESS_READ_ONLY, null);
		energyConsumptionMO = new MOScalar(MOIdentifiers.APPT_ENERGY_CONSUMPTION, MOAccessImpl.ACCESS_READ_WRITE, null);
		energyStorageMO = new MOScalar(MOIdentifiers.APPT_ENERGY_STORAGE, MOAccessImpl.ACCESS_READ_WRITE, null);
		energyGenerationMO = new MOScalar(MOIdentifiers.APPT_ENERGY_GENERATION, MOAccessImpl.ACCESS_READ_WRITE, null);
		energyGenerationBySolarMO = new MOScalar(MOIdentifiers.APPT_ENERGY_GENERATION_BY_SOLAR, MOAccessImpl.ACCESS_READ_WRITE, null);
		energyGenerationByHydroMO = new MOScalar(MOIdentifiers.APPT_ENERGY_GENERATION_BY_HYDRO, MOAccessImpl.ACCESS_READ_WRITE, null);
		
		flats = new ArrayList<Flat>();
		flatsTable();
	}
	
	public void setDeviceIdMOValue(String moValue){
		deviceIdMO.setValue(getVariable(moValue));
	}
	public void setConsumptionMOValue(String moValue){
		energyConsumptionMO.setValue(getVariable(moValue));
	}
	public void setStorageMOValue(String moValue){
		energyStorageMO.setValue(getVariable(moValue));
	}
	public void setGenerationMOValue(String moValue){
		energyGenerationMO.setValue(getVariable(moValue));
	}
	public void setGenerationBySolarMOValue(String moValue){
		energyGenerationBySolarMO.setValue(getVariable(moValue));
	}
	public void setGenerationByHydroMOValue(String moValue){
		energyGenerationByHydroMO.setValue(getVariable(moValue));
	}
	
	public MOScalar getDeviceIdMO() {
		return deviceIdMO;
	}
	public MOScalar getEnergyConsumptionMO() {
		return energyConsumptionMO;
	}
	public MOScalar getEnergyStorageMO() {
		return energyStorageMO;
	}
	public MOScalar getEnergyGenerationMO() {
		return energyGenerationMO;
	}
	public MOScalar getEnergyGenerationBySolarMO() {
		return energyGenerationBySolarMO;
	}
	public MOScalar getEnergyGenerationByHydroMO() {
		return energyGenerationByHydroMO;
	}
	
	/**
	 * The calling method will create the flat object, populate it with default
	 * and then call addFlat method. This method will first validate the flat
	 * object then add new data row in managed object table of flat.
	 * 
	 * @param flat
	 *            the flat object to be added to this apartment
	 */
	public void addFlat(Flat flat) {
		if(!flat.isValid()){
			throw new IllegalArgumentException("Flat object or its values of managed object cannot be null.");
		}
		flat.addNewRowForManagedObject(builder);
		
		if(flats.contains(flat)){
			System.out.println("Flat "+flat.getDeviceIdValue()+" is already added to "+deviceIdMO+" appartment.");
			return;
		}
		flats.add(flat);
	}
	
	/**
	 * Generate table headers of a table that will store managed object
	 * information of flat. The flat class will add actual data based on values
	 * that have been set to the flat object.
	 */
	public void flatsTable(){
		builder = new MOTableBuilder(MOIdentifiers.FLAT_BASE_OID)
			.addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_ONLY) // flat no
			.addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_WRITE) // consumption
			.addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_WRITE) // consumption by heating and cooling
			.addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_WRITE) // consumption by lighting
			.addColumnType(SMIConstants.SYNTAX_OCTET_STRING, MOAccessImpl.ACCESS_READ_WRITE); // consumption by misc
	}
	
	/**
	 * Based on aggregation of different energy sources the total energy
	 * consumption is build and updated into managed object.
	 */
	private void updateTotalEnergyGeneration(){
		String energyGenerationBySolarStr = energyGenerationBySolarMO.getValue().toString();
		String energyGenerationByHydroStr = energyGenerationByHydroMO.getValue().toString();
		String energyStorageStr = energyStorageMO.getValue().toString();
		
		int totalEnergyGeneration = 0;
		int energyGenerationBySolar = 0;
		int energyGenerationByHydro = 0;
		int energyStorage = 0;
		
		try{
			energyGenerationBySolar = Integer.parseInt(energyGenerationBySolarStr);
			energyGenerationByHydro = Integer.parseInt(energyGenerationByHydroStr);
			energyStorage = Integer.parseInt(energyStorageStr);
		} catch (NumberFormatException ex){
			System.out.println("The value set for energy generated by hydro or solar is not a integer.");
		}
		totalEnergyGeneration = energyGenerationBySolar + energyGenerationByHydro + energyStorage;
		setGenerationMOValue(""+totalEnergyGeneration);
	}
	
	/**
	 * Register the managed objects handled by this model object to Managed
	 * Object Server
	 */
	public void registerMOs(BaseAgent agent) throws DuplicateRegistrationException, InvalidAttributeValueException {
		updateTotalEnergyGeneration();
		
		// making sure that all manage objects are having appropriate default value.
		if(deviceIdMO.getValue() == null){
			throw new InvalidAttributeValueException("DeviceId manage object value cannot be null before registering manage object.");
		} else if(energyConsumptionMO.getValue() == null){
			throw new InvalidAttributeValueException("Energy consumption manage object value cannot be null before registering manage object.");
		} else if(energyStorageMO.getValue() == null){
			throw new InvalidAttributeValueException("Energy storage manage object value cannot be null before registering manage object.");
		} else if(energyGenerationMO.getValue() == null){
			throw new InvalidAttributeValueException("Energy generation manage object value cannot be null before registering manage object.");
		} else if(energyGenerationBySolarMO.getValue() == null){
			throw new InvalidAttributeValueException("Energy generation by solar manage object value cannot be null before registering manage object.");
		} else if(energyGenerationByHydroMO.getValue() == null){
			throw new InvalidAttributeValueException("Energy generation by hydro manage object value cannot be null before registering manage object.");
		}

		DefaultMOServer server = agent.getServer();
		
		// making sure that the manage object is not already registered
		server.unregister(deviceIdMO, null);
		server.unregister(energyConsumptionMO, null);
		server.unregister(energyStorageMO, null);
		server.unregister(energyGenerationMO, null);
		server.unregister(energyGenerationBySolarMO, null);
		server.unregister(energyGenerationByHydroMO, null);
		
		server.unregister(builder.build(), null);
		
		
		// its safe to register now the manage object, registering
		server.register(deviceIdMO, null);
		server.register(energyConsumptionMO, null);
		server.register(energyStorageMO, null);
		server.register(energyGenerationMO, null);
		server.register(energyGenerationBySolarMO, null);
		server.register(energyGenerationByHydroMO, null);
		
		server.register(builder.build(), null);
	}
}
