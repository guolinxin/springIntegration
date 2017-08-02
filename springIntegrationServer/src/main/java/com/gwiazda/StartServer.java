package com.gwiazda;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by michal.gwiazda on 13.08.16.
 */

@SpringBootApplication
public class StartServer {
    public static void main(String[] args) throws Exception{
        ConfigurableApplicationContext context = SpringApplication.run(StartServer.class, args);
        System.out.println("Hit enter to terminate");
        System.in.read();
        context.close();
    }

    @Bean
    public IntegrationFlow flow(RabbitTemplate rabbitTemplate){
        return IntegrationFlows.from(Http.inboundGateway("/receiveGateway")
                .requestMapping(m -> m.methods(HttpMethod.POST))
                .requestPayloadType(String.class))
                .split(commaSplitter())
                .channel(MessageChannels.executor(executor()))
                .<String, String>transform(p -> p + " from the other side")
//                .<String, String>transform(String::toUpperCase)
                .handle(Amqp.outboundGateway(rabbitTemplate).routingKey("spring-boot"))
                .aggregate()
                .transform(Object::toString)
                .get();
    }

    @Bean
    public Executor executor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(10);
        return exec;
    }

    /**
     * If your RabbitMq instance does not run on localhost with standard port 5672 you have to:
     * 1. Remove below Bean (amqp() ).
     * 2. Uncomment 2 beans that are now commented (amqp() and connectionFactory() )
     * 3. Provide your RabbitMq host and port as a parameter to CachingConnectionFactory (in place of "192.168.99.100", 5672)
     */

    @Bean
    public IntegrationFlow amqp(ConnectionFactory connectionFactory){

        return IntegrationFlows.from(Amqp.inboundGateway(connectionFactory, "spring-boot"))
                .route("payload.substring(0, 3)", r -> r
                        .resolutionRequired(false)
                        .subFlowMapping("foo", s -> s.<String, String>transform(String::toUpperCase))
                        .subFlowMapping("bar", s -> s.<String, String>transform(p -> p + p)))
//                .<String, String>transform(String::toUpperCase)
                .get();
    }

//    @Bean
//    public IntegrationFlow amqp(){
//
//        return IntegrationFlows.from(Amqp.inboundGateway(connectionFactory(), "foo"))
//                .route("payload.substring(0, 3)", r -> r
//                        .resolutionRequired(false)
//                .subFlowMapping("foo", s -> s.<String, String>transform(String::toUpperCase))
//                .subFlowMapping("bar", s -> s.<String, String>transform(p -> p + p)))
////                .<String, String>transform(String::toUpperCase)
//                .get();
//    }

//    @Bean
//    public ConnectionFactory connectionFactory() {
//        CachingConnectionFactory connectionFactory =
//                new CachingConnectionFactory("192.168.99.100", 5672);
//        connectionFactory.setUsername("guest");
//        connectionFactory.setPassword("guest");
//        return connectionFactory;
//    }

    @Bean
    DefaultMessageSplitter commaSplitter(){
        DefaultMessageSplitter splitter = new DefaultMessageSplitter();
        splitter.setDelimiters(",");
        return splitter;
    }

}
