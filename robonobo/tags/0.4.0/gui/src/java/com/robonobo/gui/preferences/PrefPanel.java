package com.robonobo.gui.preferences;

import java.beans.PropertyDescriptor;

import javax.swing.JPanel;

import com.robonobo.common.exceptions.SeekInnerCalmException;
import com.robonobo.gui.RobonoboFrame;

public abstract class PrefPanel extends JPanel {
	protected RobonoboFrame frame;
	
	public PrefPanel(RobonoboFrame frame) {
		this.frame = frame;
	}
	
	public abstract boolean hasChanged();
	
	public abstract void applyChanges();

	public abstract void resetValue();
	
	protected String getProperty(String propName) {
		String[] toks = propName.split("\\.");
		String cfgName = toks[0];
		String prop = toks[1];
		Object config = frame.getController().getConfig(cfgName);
		if(config == null)
			throw new SeekInnerCalmException();
		try {
			PropertyDescriptor propDesc = new PropertyDescriptor(prop, config.getClass());
			Object propVal = propDesc.getReadMethod().invoke(config);
			return String.valueOf(propVal);
		} catch (Exception e) {
			throw new SeekInnerCalmException(e);
		}
	}

	protected void setProperty(String propName, String value) {
		String[] toks = propName.split("\\.");
		String cfgName = toks[0];
		String prop = toks[1];
		Object config = frame.getController().getConfig(cfgName);
		if(config == null)
			throw new SeekInnerCalmException();
		try {
			PropertyDescriptor propDesc = new PropertyDescriptor(prop, config.getClass());
			Class<?> propClass = propDesc.getPropertyType();
			if(propClass.isAssignableFrom(String.class))
				propDesc.getWriteMethod().invoke(config, value);
			else if(propClass.isAssignableFrom(Integer.class) || propClass.isAssignableFrom(Integer.TYPE))
				propDesc.getWriteMethod().invoke(config, Integer.valueOf(value));
			else if(propClass.isAssignableFrom(Boolean.class) || propClass.isAssignableFrom(Boolean.TYPE))
				propDesc.getWriteMethod().invoke(config, Boolean.valueOf(value));
			else
				throw new SeekInnerCalmException();
		} catch (Exception e) {
			throw new SeekInnerCalmException(e);
		}
	}
}
