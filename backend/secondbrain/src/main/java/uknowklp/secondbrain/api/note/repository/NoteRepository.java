package uknowklp.secondbrain.api.note.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uknowklp.secondbrain.api.note.domain.Note;

public interface NoteRepository extends JpaRepository<Note, Long> {

	/**
	 * 사용자의 최근 노트 목록 조회
	 * updatedAt 기준 내림차순 정렬, 동일 시 noteId 기준 내림차순
	 * Pageable을 통해 조회 개수 제어 가능
	 *
	 * @param userId 사용자 ID
	 * @param pageable 페이지 정보 (페이지 번호, 사이즈, 정렬)
	 * @return 최근 노트 목록
	 */
	@Query("SELECT n FROM Note n WHERE n.user.id = :userId ORDER BY n.updatedAt DESC, n.id DESC")
	List<Note> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
