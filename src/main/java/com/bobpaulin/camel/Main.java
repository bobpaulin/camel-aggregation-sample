package com.bobpaulin.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class Main {
	public static void main(String[] args) throws Exception {
		CamelContext context = new DefaultCamelContext();
		
		context.addRoutes(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				restConfiguration().component("netty4-http").port(8282).bindingMode(RestBindingMode.json);
				
				rest("/test").get().to("direct:serviceFacade");
				
				
				from("direct:serviceFacade")
				  .multicast(new AggregationStrategy() {
					
					public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
						if(oldExchange == null)
						{
							return newExchange;
						}
						else{
							Map oldMap = oldExchange.getIn().getBody(Map.class);
							Map newMap = newExchange.getIn().getBody(Map.class);
							Map result = new HashMap();
							result.putAll(oldMap);
							result.putAll(newMap);
							newExchange.getIn().setBody(result);
							return newExchange;
						}
					}
				}).parallelProcessing().to("direct:ipRoute", "direct:dateRoute")
				    
				  .end();
				
				from("direct:ipRoute").to("http4://ip.jsontest.com?bridgeEndpoint=true&throwExceptionOnFailure=false").unmarshal().json(JsonLibrary.Jackson, Map.class);
				from("direct:dateRoute").to("http4://date.jsontest.com?bridgeEndpoint=true&throwExceptionOnFailure=false").unmarshal().json(JsonLibrary.Jackson, Map.class);
			}
		});
		
		context.start();
		
	}

}
