package uknowklp.secondbrain.api.note.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uknowklp.secondbrain.api.note.domain.Note;

public interface NoteRepository extends JpaRepository<Note, Long> {

	// N+1 방지를 위한 fetch join
	@Query("select n from Note n join fetch n.user where n.id = :noteId")
	Optional<Note> findByIdWithUser(@Param("noteId") Long noteId);
}
