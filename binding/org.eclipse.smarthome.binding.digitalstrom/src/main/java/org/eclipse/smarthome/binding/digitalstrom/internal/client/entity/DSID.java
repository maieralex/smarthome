/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.digitalstrom.internal.client.entity;


/**
 * The {@link DSID} represent a DigitalSTROM-Device dSID.
 * 
 * @author Alexander Betker
 * @since 1.3.0
 */
public class DSID {
	
	private String 			dsid			= null;
	private final String 	DEFAULT_DSID 	= "3504175fe000000000000001";
	private final String	PRE				= "3504175fe0000000";
	
	/**
	 * Creates a new {@link DSID} of the given dSID.
	 * 
	 * @param dsid
	 */
	public DSID(String dsid) {
		if (dsid != null && !dsid.trim().equals("")) {
			if (dsid.trim().length() == 24) {
				this.dsid = dsid;
			}
			else if (dsid.trim().length() == 8) {
				this.dsid = this.PRE+dsid;
			}
			else if (dsid.trim().toUpperCase().equals("ALL")) {
				this.dsid = "ALL";
			}
			else {
				this.dsid = DEFAULT_DSID;
			}
		}
		else {
			this.dsid = DEFAULT_DSID;
		}
	}
	
	/**
	 * Returns the value of this dSID.
	 * 
	 * @return dSID value
	 */
	public String getValue() {
		return dsid;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DSID) {
			return ((DSID)obj).getValue().equals(this.getValue());
		}
		return false;
	}

	@Override
	public int hashCode(){
		return this.getValue().hashCode();
	}
	
	@Override
	public String toString() {
		return dsid;
	}

}
