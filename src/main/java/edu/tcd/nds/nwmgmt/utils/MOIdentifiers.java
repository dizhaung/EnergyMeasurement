package edu.tcd.nds.nwmgmt.utils;

import org.snmp4j.smi.OID;

public class MOIdentifiers {
	public static final OID APPT_IDENTIFIER = new OID(".1.3.6.1.2.1.2.1.1");
	public static final OID APPT_ENERGY_CONSUMPTION = new OID(".1.3.6.1.2.1.2.2.1");
	public static final OID APPT_ENERGY_GENERATION = new OID(".1.3.6.1.2.1.2.3.1");
	public static final OID APPT_ENERGY_STORAGE = new OID(".1.3.6.1.2.1.2.4.1");
	public static final OID APPT_ENERGY_GENERATION_BY_SOLAR = new OID(".1.3.6.1.2.1.2.5.1");
	public static final OID APPT_ENERGY_GENERATION_BY_HYDRO = new OID(".1.3.6.1.2.1.2.6.1");

	public static final OID FLAT_BASE_OID = new OID(".1.3.6.1.2.1.3.1.1");
}
