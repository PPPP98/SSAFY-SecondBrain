"""API v1 라우터 통합"""
from fastapi import APIRouter
from app.api.v1.endpoints import notes, search, stats, graph

# 통합 라우터
router = APIRouter(prefix="/api/v1")

# 각 엔드포인트 라우터 포함
router.include_router(notes.router)      # /api/v1/notes/*
router.include_router(search.router)     # /api/v1/search/*
router.include_router(stats.router)      # /api/v1/stats/*
router.include_router(graph.router)      # /api/v1/graph/*

# ===== 생성되는 엔드포인트 =====
# POST   /api/v1/notes              - 노트 생성
# GET    /api/v1/notes              - 노트 목록 조회
# GET    /api/v1/notes/{note_id}    - 노트 상세 조회
# DELETE /api/v1/notes/{note_id}    - 노트 삭제
# GET    /api/v1/search/by-title    - 제목 검색
# GET    /api/v1/stats              - 통계 조회
# GET    /api/v1/graph/visualization - 그래프 시각화 데이터
# GET    /api/v1/graph/neighbors/{note_id} - 이웃 노드 조회
