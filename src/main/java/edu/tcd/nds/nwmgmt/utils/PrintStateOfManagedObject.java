package edu.tcd.nds.nwmgmt.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.snmp4j.smi.OID;

import edu.tcd.nds.nwmgmt.snmp.EnergyMeasurementManager;

public class PrintStateOfManagedObject {
	private String appartmentId;
	private String consumption;
	private String generation;
	private String storage;
	private String generationBySolar;
	private String generationByHydro;
	private List<List<String>> tableContents;

	public void print() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		buffer.append("\n+                                   STATE OF MANAGE OBJECTS                               +");
		buffer.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

		buffer.append("\n\n\n+++ APPARTMENT MANAGED OBJECT +++");
		buffer.append("\nApartment Id: " + appartmentId);
		buffer.append("\nApartment energy consumption: "+ consumption);
		buffer.append("\nApartment energy generation: " + generation);
		buffer.append("\n ++ Apartment energy storage: " + storage);
		buffer.append("\n ++ Apartment energy generation by solar: "+ generationBySolar);
		buffer.append("\n ++ Apartment energy generation by hydro: "+ generationByHydro);

		buffer.append("\n\n\n+++ FLATS MANAGED OBJECT +++");
		for (Iterator<List<String>> iterator = tableContents.iterator(); iterator.hasNext();) {
			buffer.append("\n\n+++ FLAT MANAGED OBJECT +++");
			List<String> list = (List<String>) iterator.next();
			Object[] values = list.toArray();
			buffer.append("\nFlat Id: " + values[0]);
			buffer.append("\nTotal energy consumption of flat: "+ values[1]);
			buffer.append("\n ++ Total energy consumption of flat by heating and cooling: "+ values[2]);
			buffer.append("\n ++ Total energy consumption of flat by lighting : "+ values[3]);
			buffer.append("\n ++ Total energy consumption of flat by miscellaneous : "+ values[4]);
		}
		System.out.println(buffer.toString());
	}
	
	public void setAppartmentId(String appartmentId) {
		this.appartmentId = appartmentId;
	}

	public void setConsumption(String consumption) {
		this.consumption = consumption;
	}

	public void setGeneration(String generation) {
		this.generation = generation;
	}

	public void setStorage(String storage) {
		this.storage = storage;
	}

	public void setGenerationBySolar(String generationBySolar) {
		this.generationBySolar = generationBySolar;
	}

	public void setGenerationByHydro(String generationByHydro) {
		this.generationByHydro = generationByHydro;
	}

	public void setTableContents(List<List<String>> tableContents) {
		this.tableContents = tableContents;
	}

	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("Usage: java PrintStateOfManagedObject <private_ip_address/port>");
			return;
		}

		String ipAndPort = args[0];
		EnergyMeasurementManager manager = new EnergyMeasurementManager(ipAndPort);
		PrintStateOfManagedObject printer = new PrintStateOfManagedObject();
		
		
		manager = new EnergyMeasurementManager(ipAndPort);
		OID appartmentId = new OID(MOIdentifiers.APPT_IDENTIFIER);
		OID consumption = new OID(MOIdentifiers.APPT_ENERGY_CONSUMPTION);
		OID generation = new OID(MOIdentifiers.APPT_ENERGY_GENERATION);
		OID storage = new OID(MOIdentifiers.APPT_ENERGY_STORAGE);
		OID generationBySolar = new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_SOLAR);
		OID generationByHydro = new OID(MOIdentifiers.APPT_ENERGY_GENERATION_BY_HYDRO);
		
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
			
			
			printer.setAppartmentId(appartmentIdStr);
			printer.setConsumption(consumptionStr);
			printer.setGeneration(generationStr);
			printer.setGenerationByHydro(generationByHydroStr);
			printer.setGenerationBySolar(generationBySolarStr);
			printer.setStorage(storageStr);
			printer.setTableContents(tableContents);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				manager.stop();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		printer.print();
	}
}
