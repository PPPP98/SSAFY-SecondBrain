from fastapi import HTTPException
from app.core.config import get_settings
import logging
import requests
import httpx

settings = get_settings()

logger = logging.getLogger(__name__)


class ExternalService:
    def __init__(self):
        self.api_url = settings.secondbrain_api_url

    def post_call_external_service(
        self,
        auth_token: str,
        payload: dict,
    ) -> dict:
        URL: str = f"{self.api_url}notes"
        headers = {
            "Authorization": f"Bearer {auth_token}",
            "Content-Type": "application/json",
        }
        try:
            response = requests.post(URL, json=payload, headers=headers)
            response.raise_for_status()
            return response.json()

        except Exception as e:
            logger.error(f"External service call failed: {e}")
            raise HTTPException(
                status_code=500, detail=f"External service call failed: {e}"
            )

    async def async_post_call_external_service(
        self,
        auth_token: str,
        payload: dict,
    ) -> dict:
        URL: str = f"{self.api_url}notes"
        headers = {
            "Authorization": f"Bearer {auth_token}",
            "Content-Type": "application/json",
        }
        try:
            async with httpx.AsyncClient() as client:
                response = await client.post(URL, json=payload, headers=headers)
                response.raise_for_status()
                return response.json()

        except Exception as e:
            logger.error(f"External service call failed: {e}")
            raise HTTPException(
                status_code=500, detail=f"External service call failed: {e}"
            )


external_service = ExternalService()
