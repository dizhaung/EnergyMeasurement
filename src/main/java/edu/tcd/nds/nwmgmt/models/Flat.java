package edu.tcd.nds.nwmgmt.models;

import javax.management.InvalidAttributeValueException;

import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.smi.OctetString;

import edu.tcd.nds.nwmgmt.utils.MOTableBuilder;

/**
 * A model class that represents flat in actual usecase defined by Group2
 * energy simulation for smart district project. Please check usecase in
 * documentation
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public class Flat extends BaseModel {
	private String[] flatRowData;
	
	/**
	 * Default constructor
	 */
	public Flat(){
		flatRowData = new String[5];
	}
	
	/**
	 * Make sure that the flat object is valid by checking managed object values
	 * are set properly.
	 * 
	 * @return true if all managed object values are set otherwise false
	 */
	public boolean isValid(){
		if(flatRowData[0] == null || flatRowData[1] == null || flatRowData[2] == null || flatRowData[3] == null || flatRowData[4] == null){
			return false;
		}
		return true;
	}
	
	/**
	 * Add a new row of managed objects in managed object table
	 * 
	 * @param builder
	 *            the #MOTableBuilder utility class
	 */
	public void addNewRowForManagedObject(MOTableBuilder builder){
		builder.addRowValue(new OctetString(flatRowData[0]))
		.addRowValue(new OctetString(flatRowData[1]))
		.addRowValue(new OctetString(flatRowData[2]))
		.addRowValue(new OctetString(flatRowData[3]))
		.addRowValue(new OctetString(flatRowData[4]));
	}
	
	public void setDeviceIdValue(String moValue){
		flatRowData[0] = moValue; // 0 - device id
	}
	public void setEnergyConsumptionMOValue(String moValue){
		flatRowData[1] = moValue; // 1 - energy consumption
	}
	public void setEnergyConsumptionByHeatingCoolingMOValue(String moValue){
		flatRowData[2] = moValue; // 2 - energy consumption by heating and cooling
	}
	public void setEnergyConsumptionByLightingMOValue(String moValue){
		flatRowData[3] = moValue; // 3 - energy consumption by lighting
	}
	public void setEnergyConsumptionByMiscMOValue(String moValue){
		flatRowData[4] = moValue; // 4 - energy consumption by miscellaneous 
	}	
	public String getDeviceIdValue(){
		return flatRowData[0];
	}
	
	/**
	 * Registration of flat managed object is taken care by apartment. In future
	 * use refuse bequest re-factoring patterns to avoid empty implementation.
	 */
	public void registerMOs(BaseAgent agent) throws DuplicateRegistrationException, InvalidAttributeValueException {
		// registration of flat managed object is taken care by apartment, refuse bequest re-factoring patterns. 
	}
}
