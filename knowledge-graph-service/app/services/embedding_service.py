from openai import OpenAI
from app.core.config import get_settings
import tiktoken
import logging
from typing import Tuple, List

logger = logging.getLogger(__name__)
settings = get_settings()


class EmbeddingService:
    """OpenAI 임베딩 생성"""

    def __init__(self):
        # OpenAI 클라이언트 초기화
        if settings.openai_base_url:
            # GMS 서비스 사용
            self.client = OpenAI(
                api_key=settings.openai_api_key, base_url=settings.openai_base_url
            )
            logger.debug(f"✅ OpenAI 클라이언트 (GMS): {settings.openai_base_url}")
        else:
            # 기본 OpenAI API 사용
            self.client = OpenAI(api_key=settings.openai_api_key)
            logger.debug("✅ OpenAI 클라이언트 (Base API)")

        self.model = settings.openai_model
        self.encoding = tiktoken.encoding_for_model("gpt-3.5-turbo")

    def count_tokens(self, text: str) -> int:
        """
        토큰 수 계산

        Args:
            text: 계산할 텍스트

        Returns:
            토큰 개수
        """
        return len(self.encoding.encode(text))

    def generate_embedding(self, text: str) -> Tuple[List[float], int]:
        """
        임베딩 생성 (전체 내용, 제한 없음)

        Args:
            text: 임베딩할 텍스트 (전체 노트 내용)

        Returns:
            (임베딩 벡터, 토큰 개수)

        Raises:
            Exception: OpenAI API 호출 실패
        """
        try:
            # 1. 토큰 수 계산 (정보용)
            token_count = self.count_tokens(text)
            logger.debug(f"📊 토큰 수: {token_count}개")

            # 2. OpenAI API 호출 (제한 없이 전체 임베딩)
            logger.debug(f"🤖 임베딩 생성 중...")

            response = self.client.embeddings.create(
                model=self.model,
                input=text,  # 👈 전체 내용 그대로
                encoding_format="float",
            )

            # 3. 임베딩 추출
            embedding = response.data[0].embedding

            logger.debug(f"✅ 임베딩 생성 완료: {len(embedding)}차원")

            return embedding, token_count

        except Exception as e:
            logger.error(f"❌ 임베딩 생성 실패: {e}")
            raise


# 싱글톤 인스턴스
embedding_service = EmbeddingService()
