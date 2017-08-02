package com.gwiazda;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.http.Http;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Created by michal.gwiazda on 15.08.16.
 */

@SpringBootApplication
@IntegrationComponentScan
public class StartClientB {

    private static final String QUEUE = "spring-boot";

    private final CountDownLatch listenLatch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = SpringApplication.run(StartClientB.class, args);
        Gateway gate = context.getBean(Gateway.class);
        Scanner scanner = new Scanner(System.in);
        while(scanner.hasNext()){
            String line = scanner.nextLine();
            System.out.println(gate.exchange(line));
        }
        scanner.close();
    }

    @RabbitListener(queues = QUEUE)
    public void listen(String in) {
        System.out.println("Listener received: " + in);
        this.listenLatch.countDown();
    }

    @MessagingGateway(defaultRequestChannel = "httpOut.input")
    public interface Gateway {
        public String exchange(String out);
    }

    @Bean
    IntegrationFlow httpOut(){
      return f -> f.handle(Http.outboundGateway("http://localhost:8880/receiveGateway")
      .expectedResponseType(String.class));
    }

}
