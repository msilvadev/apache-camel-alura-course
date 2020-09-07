package br.com.caelum.camel.spring;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Service;

@Service
public class OrderService extends RouteBuilder{
	
    @Override
    public void configure() throws Exception {
        from("file:orders").
        to("activemq:queue:orders");
    }
}
