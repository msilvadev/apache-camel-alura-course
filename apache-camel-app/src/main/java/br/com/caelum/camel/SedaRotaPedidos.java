package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class SedaRotaPedidos {
	public static void main(String[] args) throws Exception {
		
		// CAMEL É UMA ROUTING ENGINE
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				/*
				 * Há uma alternativa ao direct e multicast. Na rota e sub-rotas podemos aplicar
				 * algo chamado de Staged event-driven architecture ou simplesmente SEDA. A
				 * ideia do SEDA é que cada rota (e sub-rota) possua uma fila dedicada de
				 * entrada e as rotas enviam mensagens para essas filas para se comunicar.
				 * Dentro dessa arquitetura, as mensagens são chamadas de eventos. A rota fica
				 * então consumindo as mensagens/eventos da fila, tudo funcionando em paralelo.
				 */
				from("file:pedidos?delay=5s&noop=true").
			    	routeId("rota-pedidos").
			    to("seda:soap").
			    to("seda:http");

				from("seda:soap").
				    routeId("rota-soap").
				    log("chamando servico soap ${body}").
				to("mock:soap");

				from("seda:http").
				    routeId("rota-http").
				    setProperty("pedidoId", xpath("/pedido/id/text()")).
				    setProperty("email", xpath("/pedido/pagamento/email-titular/text()")).
				    split().
				        xpath("/pedido/itens/item").
				    filter().
				        xpath("/item/formato[text()='EBOOK']").
				    setProperty("ebookId", xpath("/item/livro/codigo/text()")).
				    setHeader(Exchange.HTTP_QUERY,
				            simple("clienteId=${property.email}&pedidoId=${property.pedidoId}&ebookId=${property.ebookId}")).
				to("http4://localhost:8080/webservices/ebook/item");
				
				from("direct:soap").
					routeId("rota-soap").
					log("RouteId ${routeId} - messageId ${id}").
					setBody(constant("<envelope>teste</envelope>")).
				to("mock:soap");
			}
		});	
		
		context.start();
		Thread.sleep(20_000);
		context.stop();
	}	
}