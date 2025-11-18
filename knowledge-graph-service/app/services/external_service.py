from fastapi import HTTPException
from app.core.config import get_settings
import logging
import httpx
from typing import Dict

settings = get_settings()

logger = logging.getLogger(__name__)


class ExternalService:
    def __init__(self):
        self.api_url = settings.secondbrain_api_url
        self._client = None

    async def get_client(self) -> httpx.AsyncClient:
        """singleton client"""
        if self._client is None or self._client.is_closed:
            self._client = httpx.AsyncClient(timeout=180.0)
        return self._client

    async def close(self):
        """close client"""
        if self._client is not None:
            await self._client.aclose()

    async def async_post_call_external_service(
        self,
        auth_token: str,
        payload: Dict,
    ) -> Dict:
        URL: str = f"{self.api_url}notes"
        headers = {
            "Authorization": f"Bearer {auth_token}",
            "Content-Type": "application/json",
        }
        try:
            client = await self.get_client()
            response = await client.post(URL, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()

        except httpx.RequestError as e:
            logger.error(f"Network error : {e}")
            raise HTTPException(
                status_code=503, detail=f"External service call failed: {e}"
            )
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error : {e}")
            raise HTTPException(status_code=e.response.status_code, detail=str(e))

        except Exception as e:
            logger.error(f"error : {e}")
            raise HTTPException(status_code=500, detail=f"External service call error")

    async def get_user_id(
        self,
        api_key: str,
    ) -> Dict:
        URL: str = f"{self.api_url}apikey/validate"
        payload = {"apiKey": api_key}
        try:
            client = await self.get_client()
            response = await client.post(URL, json=payload)
            response.raise_for_status()
            return response.json()

        except httpx.RequestError as e:
            logger.error(f"Network error : {e}")
            raise HTTPException(
                status_code=503, detail=f"External service call failed: {e}"
            )
        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error : {e}")
            raise HTTPException(status_code=e.response.status_code, detail=str(e))

        except Exception as e:
            logger.error(f"error : {e}")
            raise HTTPException(status_code=500, detail=f"External service call error")

    async def get_note_data(
        self,
        api_key: str,
        note_id: int,
    ) -> Dict:
        URL: str = f"{self.api_url}mcp/notes/{note_id}"
        try:
            client = await self.get_client()
            response = await client.get(URL, headers={"X-API-Key": api_key})
            response.raise_for_status()
            return response.json()

        except httpx.RequestError as e:
            logger.error(f"Network error while fetching note {note_id}: {e}")
            raise HTTPException(
                status_code=503,
                detail=f"External service call failed: {e}",
            )

        except httpx.HTTPStatusError as e:
            logger.error(f"HTTP error while fetching note {note_id}: {e}")

            # 404 처리
            if e.response.status_code == 404:
                raise HTTPException(
                    status_code=404,
                    detail=f"Note {note_id} not found",
                )

            raise HTTPException(
                status_code=e.response.status_code,
                detail=str(e),
            )

        except Exception as e:
            logger.error(f"Error fetching note {note_id}: {e}")
            raise HTTPException(
                status_code=500,
                detail="External service call error",
            )


external_service = ExternalService()
