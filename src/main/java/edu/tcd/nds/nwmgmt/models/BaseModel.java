package edu.tcd.nds.nwmgmt.models;

import javax.management.InvalidAttributeValueException;

import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

/**
 * A base model class that every model object need to extends
 * 
 * @author Sachin Hadke and Farhan Ahmad
 *
 */
public abstract class BaseModel {
	/**
	 * The implementing model class must implement this method and register the
	 * managed object that the given model class is responsible to handling
	 */
	public abstract void registerMOs(BaseAgent agent) throws DuplicateRegistrationException, InvalidAttributeValueException;
	
	/**
	 * Given a String value return a Variable object that is used as value
	 * parameter of managed object by snmp4j
	 * 
	 * @param value
	 *            the input to be set to managed object
	 * @return the Variable object that is used as value parameter of managed
	 *         object by snmp4j
	 */
	protected Variable getVariable(String value) {
		if(value == null){
			throw new IllegalArgumentException("The measured object value cannot be null.");
		}
		
		return new OctetString(value);
	}
}
