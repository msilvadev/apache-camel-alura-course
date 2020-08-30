package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
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
				
				// PADRÃO FILE SHARING
				//nomeDoComponente:nomedapasta?parametros
				from("file:pedidos?delay=5s&noop=true").
					split().
						xpath("/pedido/itens/item").
					filter().
						xpath("/item/formato[text()='EBOOK']").
					// o camel lê o arquivo e internamente transforma em uma mensagem e gera um id
				    // o ID do log é o ID que o Camel gerou
					marshal().xmljson().
					log("${body}").
					setHeader(Exchange.FILE_NAME, simple("${file:name.noext}-${header.CamelSplitIndex}.json")).
				to("file:saida");
			}
		});
		
		context.start();
		Thread.sleep(20_000);
		context.stop();
	}	
}