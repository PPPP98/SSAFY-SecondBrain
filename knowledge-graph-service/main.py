from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging
from app.core.config import get_settings
from app.db.init_db import initialize_schema
from app.db.neo4j_client import neo4j_client
from app.api.v1 import router # noqa: F401

# 로깅 설정
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# 설정 로드
settings = get_settings()


# ===== 라이프사이클 관리 =====
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    앱 시작/종료 시 실행

    yield 전: 시작 시 실행
    yield 후: 종료 시 실행
    """
    # ===== 앱 시작 =====
    logger.info("🚀 Start Knowledge-graph-service")

    # 1. Neo4j 연결 확인
    try:
        if neo4j_client.verify_connection():
            logger.info("✅ Neo4j 연결 성공")
        else:
            logger.error("❌ Neo4j 연결 실패")
            raise Exception("Neo4j 연결 실패")
    except Exception as e:
        logger.error(f"❌ Neo4j 연결 오류: {e}")
        raise

    # 2. 스키마 초기화
    try:
        if initialize_schema():
            logger.info("✅ Neo4j 스키마 초기화 완료")
        else:
            logger.error("❌ Neo4j 스키마 초기화 실패")
    except Exception as e:
        logger.error(f"❌ 스키마 초기화 오류: {e}")

    yield  # 앱 실행

    # ===== 앱 종료 =====
    logger.info("🛑 애플리케이션 종료")
    try:
        neo4j_client.close()
        logger.info("✅ Neo4j 연결 종료")
    except Exception as e:
        logger.error(f"❌ Neo4j 연결 종료 오류: {e}")


# FastAPI 앱 생성 (lifespan 파라미터 추가)
app = FastAPI(
    title="Knowledge Graph Service",
    description="Neo4j 기반 지식 그래프 서비스",
    version="1.0.0",
    lifespan=lifespan,  # 라이프사이클 관리
)

# CORS 설정 (Spring Boot와 통신)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 추후 수정
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ===== 헬스 체크 =====
@app.get("/health")
async def health_check():
    """서비스 상태 확인"""
    return {
        "status": "ok",
        "service": "knowledge-graph-service",
        "version": "1.0.0",
    }


@app.get("/")
async def root():
    """루트 엔드포인트"""
    return {
        "message": "Knowledge Graph Service API",
        "docs": "/docs",
        "version": "1.0.0",
    }


# ===== 글로벌 예외 처리 =====
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """모든 예외 처리"""
    logger.error(f"예외 발생: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal Server Error",
            "message": str(exc),
        },
    )


# ===== 라우터 import =====
app.include_router(router)


if __name__ == "__main__":
    import uvicorn

    # 개발 서버 실행
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info",
    )
