/*
 *   Copyright 2018 The Portico Project
 *
 *   This file is part of portico.
 *
 *   portico is free software; you can redistribute it and/or modify
 *   it under the terms of the Common Developer and Distribution License (CDDL) 
 *   as published by Sun Microsystems. For more information see the LICENSE file.
 *   
 *   Use of this software is strictly AT YOUR OWN RISK!!!
 *   If something bad happens you do not have permission to come crying to me.
 *   (that goes for your lawyer as well)
 *
 */
package org.portico2.rti;

import org.apache.logging.log4j.Logger;
import org.portico.lrc.compat.JConfigurationException;
import org.portico.lrc.compat.JException;
import org.portico.lrc.compat.JRTIinternalError;
import org.portico.utils.messaging.PorticoMessage;
import org.portico2.common.PorticoConstants;
import org.portico2.common.configuration.ConnectionConfiguration;
import org.portico2.common.configuration.ConnectionType;
import org.portico2.common.messaging.MessageContext;
import org.portico2.common.network.ConnectionFactory;
import org.portico2.common.network.IConnection;
import org.portico2.common.network.IMessageReceiver;
import org.portico2.common.services.federation.msg.RtiProbe;

public class RtiConnection implements IMessageReceiver
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTI rti;
	private ConnectionConfiguration configuration;
	private IConnection connection;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public RtiConnection( RTI rti, ConnectionConfiguration configuration ) throws JConfigurationException
	{
		this.rti = rti;
		this.configuration = configuration;
		
		// Create the underlying connection and configure it
		this.connection = ConnectionFactory.createConnection( configuration.getType() );
		this.connection.configure( configuration, this );
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	public void connect()
	{
		// 1. Tell the connection to attach
		this.connection.connect();
		
		// 2. Check to see if there is an RTI out there
		MessageContext context = new MessageContext( new RtiProbe() );
		sendControlRequest( context );
		if( context.isSuccessResponse() )
		{
			// RTI is present
			disconnect();
			throw new JRTIinternalError( "An RTI is already running" );
		}
	}
	
	public void disconnect()
	{
		// 1. Tell the connection to detatch
		this.connection.disconnect();
	}

	///////////////////////////////////////////////////////////////////////////////////////
	///  Message SENDING methods   ////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * FIXME
	 */
	public void sendControlRequest( MessageContext context ) throws JRTIinternalError
	{
		this.connection.sendControlRequest( context );
	}
	
	/**
	 * FIXME
	 */
	public void sendDataMessage( PorticoMessage message ) throws JException
	{
		this.connection.sendDataMessage( message );
	}


	///////////////////////////////////////////////////////////////////////////////////////
	///  Message RECEIVING methods   //////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	/**
	 * FIXME
	 */
	@Override
	public void receiveControlRequest( MessageContext context ) throws JRTIinternalError
	{
		// pass the message to the RTI for processing
		rti.getInbox().receiveControlMessage( context, this );
		
		// return the resulting response message, but check it first
		if( context.hasResponse() == false )
			context.error( new JRTIinternalError("No response after passing to RTI") );
	}
	
	/**
	 * FIXME
	 */
	@Override
	public void receiveDataMessage( PorticoMessage incoming ) throws JException
	{
		rti.getInbox().receiveDataMessage( incoming, this );
	}
	
	@Override
	public final boolean isReceivable( int targetFederate )
	{
		return targetFederate == PorticoConstants.RTI_HANDLE ||
		       targetFederate == PorticoConstants.TARGET_ALL_HANDLE;
	}

	@Override
	public Logger getLogger()
	{
		return rti.getLogger();
	}

	////////////////////////////////////////////////////////////////////////////////////////
	///  Accessors and Mutators   //////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////
	public String getName()
	{
		return configuration.getName();
	}
	
	public ConnectionType getType()
	{
		return configuration.getType();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}