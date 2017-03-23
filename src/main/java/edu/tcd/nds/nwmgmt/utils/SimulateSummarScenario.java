package edu.tcd.nds.nwmgmt.utils;

import java.io.IOException;

import org.snmp4j.smi.OID;

import edu.tcd.nds.nwmgmt.snmp.EnergyMeasurementManager;

public class SimulateSummarScenario {
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Usage: java SimulateSummarScenario <private_ip_address/port>");
			return;
		}
		String ipWithPort = args[0];
		
		EnergyMeasurementManager manager = null;
		OID generationBySolarOID = new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_SOLAR);
		String generationBySolarValue = "40";
		
		try { 
			manager = new EnergyMeasurementManager(ipWithPort);
			manager.setAsString(generationBySolarOID, generationBySolarValue);
			manager.sendSolarEnergyGenerationTrap(generationBySolarOID, generationBySolarValue); // solar power generation increases from 20 to 40 during summer
		} catch (IOException ex){
			ex.printStackTrace();
		} finally {
			try {
				manager.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
