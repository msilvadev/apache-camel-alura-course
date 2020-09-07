package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * @author msilvadev
 */
public class RotaPedidos {

	public static void main(String[] args) throws Exception {
		
		// CAMEL É UMA ROUTING ENGINE
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				
				errorHandler(deadLetterChannel("file:erro").
						logExhaustedMessageHistory(true).
							maximumRedeliveries(3).
								redeliveryDelay(2000). // wait 2 seconds
									onRedelivery(new Processor() {
										@Override
										public void process(Exchange exchange) throws Exception{
											int counter = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
											int max = (int) exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
											System.out.println("Redelivery " + counter + "/" + max);
										}
									})
								);
				
				// PADRÃO FILE SHARING
				//nomeDoComponente:nomedapasta?parametros
				from("file:pedidos?delay=5s&noop=true").
					log("file:name").
					routeId("rota-pedidos").
					to("validator:pedido.xsd");
//				multicast(). // pega a mensagem do camel e faz um multicast para as subrotas
//					parallelProcessing(). //Configuração que o multicast() possui para processar cada subrota em uma Thread separada
//						to("direct:http"). // direct -> forma que o CAMEL chama uma subrota
//						to("direct:soap");
				
				from("direct:http").
					routeId("rota-http").
					setProperty("pedidoId", xpath("/pedido/id/text()")).
					setProperty("clienteId", xpath("/pedido/pagamento/email-titular/text()")).
					split().
						xpath("/pedido/itens/item").
					filter().
						xpath("/item/formato[text()='EBOOK']").
					setProperty("ebookId", xpath("/item/livro/codigo/text()")).
					// o camel lê o arquivo e internamente transforma em uma mensagem e gera um id
				    // o ID do log é o ID que o Camel gerou
					marshal().xmljson().
					//log("${body}").
					setHeader(Exchange.HTTP_METHOD, HttpMethods.POST).
				to("http4://localhost:8080/webservices/ebook/item").
					setHeader(Exchange.HTTP_METHOD, HttpMethods.GET).
					setHeader(Exchange.HTTP_QUERY, simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.clienteId}")).
				to("http4://localhost:8080/webservices/ebook/item");
				
				from("direct:soap").
					routeId("rota-soap").
					log("RouteId [${routeId}] - messageId [${id}] - sending order...").
					to("xslt:pedido-para-soap.xslt"). // Message Translator - EIP
					setHeader(Exchange.CONTENT_TYPE, constant("text/xml")).
				to("http4://localhost:8080/webservices/financeiro");
			}
		});	
		
		context.start();
		Thread.sleep(20_000);
		context.stop();
	}	
}
