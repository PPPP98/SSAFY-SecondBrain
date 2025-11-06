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
			rabbitTemplate.convertAndSend(EXCHANGE_NAME, "note.created", event);
		} catch (Exception e) {
			// 지식 그래프 발행 실패해도 메인 로직엔 영향 주면 안됨
			log.error("지식 그래프 발행 실패", e);
		}
	}

	// 노트 수정 이벤트 발행
	public void publishNoteUpdated(Long noteId, Long userId, String oldTitle, String newTitle, String oldContent, String newContent) {
		try {
			// title 변경 여부 확인 (변경 없으면 null)
			String titleToSend = (oldTitle != null && oldTitle.equals(newTitle)) ? null : newTitle;

			// Content 변경 여부 확인 (변경 없으면 null)
			String contentToSend = (oldContent != null && oldContent.equals(newContent)) ? null : newContent;

			KnowledgeGraphEvent event = KnowledgeGraphEvent.updated(noteId, userId, titleToSend, contentToSend);
			rabbitTemplate.convertAndSend(EXCHANGE_NAME, "note.updated", event);
		} catch (Exception e) {
			log.error("지식 그래프 수정 이벤트 발행 실패", e);
		}
	}

	// 노트 삭제 이벤트 발행
	public void publishNoteDeleted(Long noteId, Long userId) {
		try {
			KnowledgeGraphEvent event = KnowledgeGraphEvent.deleted(noteId, userId);
			rabbitTemplate.convertAndSend(EXCHANGE_NAME, "note.deleted", event);
		} catch (Exception e) {
			log.error("지식 그래프 삭제 이벤트 발행 실패", e);
		}
	}
}
