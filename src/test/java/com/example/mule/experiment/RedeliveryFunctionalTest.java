package com.example.mule.experiment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class RedeliveryFunctionalTest extends FunctionalTestCase {

	JmsTemplate vmJms;
	JmsTemplate tcpJms;

	@Override
	protected String getConfigResources() {
		return "amq-redelivery.xml";
	}

	@Before
	public void initJmsClients() {
		ActiveMQConnectionFactory vmConnectionFactory = muleContext.getRegistry().lookupObject("amqVmConnectionFactory");
		vmJms = new JmsTemplate(vmConnectionFactory);
		ActiveMQConnectionFactory tcpConnectionFactory = muleContext.getRegistry().lookupObject("amqTcpConnectionFactory");
		UserCredentialsConnectionFactoryAdapter credentialsAdapter = new UserCredentialsConnectionFactoryAdapter();
		credentialsAdapter.setTargetConnectionFactory(tcpConnectionFactory);
		credentialsAdapter.setUsername("mule");
		credentialsAdapter.setPassword("mule");
		tcpJms = new JmsTemplate(credentialsAdapter);
	}

	@Test
	public void messageShouldErrorAtFirstButProceedAfterDelay_usingVm() throws MuleException, JMSException {
//		vmJms.convertAndSend("the.queue", "Information");

        vmJms.send("the.queue", new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                Message message = session.createTextMessage("the message body");
                message.setStringProperty("DoStuff", "foobar");

                return message;
            }
        });

		MuleMessage message = muleContext.getClient().request("vm://vm.success", 100);
		assertNull("The message should not have been received because this is the first attempt", message);
		message = muleContext.getClient().request("vm://vm.success", 3000);
		assertNotNull("The message should have been received because we have waited longer than the redelivery delay", message);
		assertEquals("JMSRedelivered header", true, message.getInboundProperty("JMSRedelivered"));
	}

	@Test
	public void messageShouldErrorAtFirstButProceedAfterDelay_usingStandalone() throws MuleException, JMSException {
		tcpJms.convertAndSend("the.queue", "Information");

		MuleMessage message = muleContext.getClient().request("vm://tcp.success", 100);
		assertNull("The message should not have been received because this is the first attempt", message);
		message = muleContext.getClient().request("vm://tcp.success", 3000);
		assertNotNull("The message should have been received because we have waited longer than the redelivery delay", message);
		assertEquals("JMSRedelivered header", true, message.getInboundProperty("JMSRedelivered"));
	}
}
