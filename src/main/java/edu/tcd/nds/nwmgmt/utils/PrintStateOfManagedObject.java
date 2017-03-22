package edu.tcd.nds.nwmgmt.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.snmp4j.smi.OID;

import edu.tcd.nds.nwmgmt.snmp.EnergyMeasurementManager;

public class PrintStateOfManagedObject {
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Usage: java PrintStateOfManagedObject <private_ip_address/port>");
			return;
		}

		String ipAndPort = args[0];
		EnergyMeasurementManager manager = null;

		try {
			manager = new EnergyMeasurementManager(ipAndPort);
			OID appartmentId = new OID(MOIdentifiers.APPT_IDENTIFIER);
			OID consumption = new OID(MOIdentifiers.APPT_ENERGY_CONSUMPTION);
			OID generation = new OID(MOIdentifiers.APPT_ENERGY_GENERATION);
			OID storage = new OID(MOIdentifiers.APPT_ENERGY_STORAGE);
			OID generationBySolar = new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_SOLAR);
			OID generationByHydro = new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_HYDRO);

			// get default value
			try {
				String appartmentIdStr = manager.getAsString(appartmentId);
				String consumptionStr = manager.getAsString(consumption);
				String generationStr = manager.getAsString(generation);
				String storageStr = manager.getAsString(storage);
				String generationBySolarStr = manager.getAsString(generationBySolar);
				String generationByHydroStr = manager.getAsString(generationByHydro);

				List<List<String>> tableContents = manager.getTableAsStrings(new OID[]{
		    			new OID(MOIdentifiers.FLAT_BASE_OID + ".1"),
		    			new OID(MOIdentifiers.FLAT_BASE_OID + ".2"),
		    			new OID(MOIdentifiers.FLAT_BASE_OID + ".3"),
		    			new OID(MOIdentifiers.FLAT_BASE_OID + ".4"),
		    			new OID(MOIdentifiers.FLAT_BASE_OID + ".5")});


				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				System.out.println("+                                   STATE OF MANAGE OBJECTS                               +");
				System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

				System.out.println("\n\n+++ APPARTMENT MANAGED OBJECT +++");
				System.out.println("Apartment Id: " + appartmentIdStr);
				System.out.println("Apartment energy consumption: " + consumptionStr);
				System.out.println("Apartment energy generation: " + generationStr);
				System.out.println(" ++ Apartment energy storage: " + storageStr);
				System.out.println(" ++ Apartment energy generation by solar: " + generationBySolarStr);
				System.out.println(" ++ Apartment energy generation by hydro: " + generationByHydroStr);

				System.out.println("\n\n+++ FLATS MANAGED OBJECT +++");
				for (Iterator<List<String>> iterator = tableContents.iterator(); iterator.hasNext();) {
					System.out.println("\n+++ FLAT MANAGED OBJECT +++");
					List<String> list = (List<String>) iterator.next();
					Object[] values = list.toArray();
					System.out.println("Flat Id: "+values[0]);
					System.out.println("Total energy consumption of flat: "+values[1]);
					System.out.println(" ++ Total energy consumption of flat by heating and cooling: "+values[2]);
					System.out.println(" ++ Total energy consumption of flat by lighting : "+values[3]);
					System.out.println(" ++ Total energy consumption of flat by miscellaneous : "+values[4]);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			try {
				manager.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
