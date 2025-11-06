package uknowklp.secondbrain.api.note.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uknowklp.secondbrain.api.note.dto.KnowledgeGraphEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphProducerService {

	private final RabbitTemplate rabbitTemplate;
	private static final String EXCHANGE_NAME = "knowledge_graph_events";

	// 노트 생성 이벤트 발행
	public void publishNoteCreated(Long noteId, Long userId, String title, String content) {
		try {
			KnowledgeGraphEvent event = KnowledgeGraphEvent.created(noteId, userId, title, content);
		}
	}
}
