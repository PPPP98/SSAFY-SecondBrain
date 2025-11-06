package uknowklp.secondbrain.global.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

	// 리마인더 메시지를 저장하는 큐, consumer가 가져갈 때까지 보관하는 버퍼
	@Bean
	public Queue reminderQueue(){
		return QueueBuilder.durable("reminder.queue")
			.build();
	}

	// Delayed Exchange 사용, delay 시간 만료 후 큐로 전달
	@Bean
	public CustomExchange delayedExchange(){
		Map<String, Object> args = new HashMap<>();
		args.put("x-delayed-type", "direct");

		return new CustomExchange(
			"reminder.exchange",
			"x-delayed-message",
			true,
			false,
			args
		);
	}

	// Exchange와 Queue를 라우팅 키로 연결
	@Bean
	public Binding binding(Queue reminderQueue, CustomExchange delayedExchange){
		return BindingBuilder
			.bind(reminderQueue)
			.to(delayedExchange)
			.with("reminder.routing.key")
			.noargs();
	}

	// Json 메시지 변환기
	@Bean
	public MessageConverter jsonMessageConverter(){
		return new Jackson2JsonMessageConverter();
	}

	// RabbitTemplate 설정, producer가 메시지 발송할 때 사용
	@Bean
	public RabbitTemplate rabbitTemplate(
		ConnectionFactory connectionFactory,
		MessageConverter jsonMessageConverter
	) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter);
		return template;
	}

	// 지식 그래프 이벤트 저장하는 큐
	@Bean
	public Queue knowledgeGraphQueue(){
		return QueueBuilder.durable("knowledge_graph.queue")
			.build();
	}

	// 지식 그래프 이벤트용 Direct Exchange
	@Bean
	public DirectExchange knowledgeGraphExchange(){
		return new DirectExchange("knowledge_graph_events", true, false);
	}

	// note created 바인딩
	@Bean
	public Binding noteCreatedBinding(Queue reminderQueue, DirectExchange knowledgeGraphExchange){
		return BindingBuilder
			.bind(knowledgeGraphExchange)
			.to(knowledgeGraphExchange)
			.with("note.created");
	}

	// note updated 바인딩
	@Bean
	public Binding noteUpdatedBinding(Queue knowledgeGraphQueue, DirectExchange knowledgeGraphExchange){
		return BindingBuilder
			.bind(knowledgeGraphQueue)
			.to(knowledgeGraphExchange)
			.with("note.updated");
	}

	// note deleted 바인딩
	@Bean
	public Binding noteDeletedBinding(Queue reminderQueue, DirectExchange knowledgeGraphExchange,
		Queue knowledgeGraphQueue){
		return BindingBuilder
			.bind(knowledgeGraphQueue)
			.to(knowledgeGraphExchange)
			.with("note.deleted");
	}
}
