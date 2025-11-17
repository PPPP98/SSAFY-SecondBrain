package uknowklp.secondbrain.api.note.dto;

public record VectorSearchResult(
	Long noteId,
	String title,
	Double similarityScore
) {
}
