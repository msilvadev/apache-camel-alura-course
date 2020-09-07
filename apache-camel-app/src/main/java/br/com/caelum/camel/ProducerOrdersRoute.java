package br.com.caelum.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * This route read files in orders folder and send as a message in queue on ActiveMQ
 * @author msilvadev
 */
public class ProducerOrdersRoute {

	public static void main(String[] args) throws Exception {
		
		// CAMEL IS A ROUTING ENGINE
		CamelContext context = new DefaultCamelContext();
		
		context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tcp://localhost:61616/"));
		
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("file:orders?noop=true").
				log("Read file ${file:name}").
				to("activemq:queue:orders");
			}
		});	
		
		context.start();
		Thread.sleep(20_000);
		context.stop();
	}	

}
